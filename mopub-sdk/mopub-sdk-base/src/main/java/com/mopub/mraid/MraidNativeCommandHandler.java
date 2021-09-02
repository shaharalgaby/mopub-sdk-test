// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import android.app.Activity;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MoPubHttpUrlConnection;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.ResponseHeader;
import com.mopub.common.util.Streams;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.util.ResponseHeader.LOCATION;

public class MraidNativeCommandHandler {
    interface MraidCommandFailureListener {
        void onFailure(MraidCommandException exception);
    }

    private static final int MAX_NUMBER_DAYS_IN_MONTH = 31;
    private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd'T'HH:mm:ssZZZZZ",
            "yyyy-MM-dd'T'HH:mmZZZZZ"
    };

    public static final String ANDROID_CALENDAR_CONTENT_TYPE = "vnd.android.cursor.item/event";

    boolean isTelAvailable(Context context) {
        return false; // No longer supported as of MoPub SDK 5.9.0
    }

    boolean isSmsAvailable(Context context) {
        return false; // No longer supported as of MoPub SDK 5.9.0
    }

    public static boolean isStorePictureSupported(Context context) {
        return false; // No longer supported as of MoPub SDK 5.9.0
    }

    static boolean isCalendarAvailable(Context context) {
        return false; // No longer supported as of MoPub SDK 5.9.0
    }

    /**
     * Inline video support was added in 3.1.
     *
     * Checks that the hardware acceleration flag is set for banners.
     *
     * Returns true if the current window has hardware acceleration enabled.
     *
     * Note: We don't check the view for hardware acceleration because that is done by MraidController.
     */
    boolean isInlineVideoAvailable(@NonNull Activity activity, @NonNull View view) {
        if (activity.getWindow() == null) {
            return false;
        }

        final int flags = activity.getWindow().getAttributes().flags;

        return (flags & WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED) != 0;
    }

    private Map<String, Object> translateJSParamsToAndroidCalendarEventMapping(Map<String, String> params) {
        Map<String, Object> validatedParamsMapping = new HashMap<String, Object>();
        if (!params.containsKey("description") || !params.containsKey("start")) {
            throw new IllegalArgumentException("Missing start and description fields");
        }

        validatedParamsMapping.put(CalendarContract.Events.TITLE, params.get("description"));

        if (params.containsKey("start") && params.get("start") != null) {
            Date startDateTime = parseDate(params.get("start"));
            if (startDateTime != null) {
                validatedParamsMapping.put(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDateTime.getTime());
            } else {
                throw new IllegalArgumentException("Invalid calendar event: start time is malformed. Date format expecting (yyyy-MM-DDTHH:MM:SS-xx:xx) or (yyyy-MM-DDTHH:MM-xx:xx) i.e. 2013-08-14T09:00:01-08:00");
            }
        } else {
            throw new IllegalArgumentException("Invalid calendar event: start is null.");
        }

        if (params.containsKey("end") && params.get("end") != null) {
            Date endDateTime = parseDate(params.get("end"));
            if (endDateTime != null) {
                validatedParamsMapping.put(CalendarContract.EXTRA_EVENT_END_TIME, endDateTime.getTime());
            } else {
                throw new IllegalArgumentException("Invalid calendar event: end time is malformed. Date format expecting (yyyy-MM-DDTHH:MM:SS-xx:xx) or (yyyy-MM-DDTHH:MM-xx:xx) i.e. 2013-08-14T09:00:01-08:00");
            }
        }

        if (params.containsKey("location")) {
            validatedParamsMapping.put(CalendarContract.Events.EVENT_LOCATION, params.get("location"));
        }

        if (params.containsKey("summary")) {
            validatedParamsMapping.put(CalendarContract.Events.DESCRIPTION, params.get("summary"));
        }

        if (params.containsKey("transparency")) {
            validatedParamsMapping.put(
                    CalendarContract.Events.AVAILABILITY,
                    params.get("transparency").equals("transparent") ?
                            CalendarContract.Events.AVAILABILITY_FREE :
                            CalendarContract.Events.AVAILABILITY_BUSY
            );
        }

        validatedParamsMapping.put(CalendarContract.Events.RRULE, parseRecurrenceRule(params));

        return validatedParamsMapping;
    }

    private Date parseDate(String dateTime) {
        Date result = null;
        for (final String DATE_FORMAT : DATE_FORMATS) {
            try {
                result = new SimpleDateFormat(DATE_FORMAT, Locale.US).parse(dateTime);
                if (result != null) {
                    break;
                }
            } catch (ParseException e) {
                // an exception is okay, just try the next format and find the first one that works
            }
        }
        return result;
    }

    private String parseRecurrenceRule(Map<String, String> params) throws IllegalArgumentException {
        StringBuilder rule = new StringBuilder();
        if (params.containsKey("frequency")) {
            String frequency = params.get("frequency");
            int interval = -1;
            if (params.containsKey("interval")) {
                interval = Integer.parseInt(params.get("interval"));
            }
            if ("daily".equals(frequency)) {
                rule.append("FREQ=DAILY;");
                if (interval != -1) {
                    rule.append("INTERVAL=" + interval + ";");
                }
            } else if ("weekly".equals(frequency)) {
                rule.append("FREQ=WEEKLY;");
                if (interval != -1) {
                    rule.append("INTERVAL=" + interval + ";");
                }
                if (params.containsKey("daysInWeek")) {
                    String weekdays = translateWeekIntegersToDays(params.get("daysInWeek"));
                    if (weekdays == null) {
                        throw new IllegalArgumentException("invalid ");
                    }
                    rule.append("BYDAY=" + weekdays + ";");
                }
            } else if ("monthly".equals(frequency)) {
                rule.append("FREQ=MONTHLY;");
                if (interval != -1) {
                    rule.append("INTERVAL=" + interval + ";");
                }
                if (params.containsKey("daysInMonth")) {
                    String monthDays = translateMonthIntegersToDays(params.get("daysInMonth"));
                    if (monthDays == null) {
                        throw new IllegalArgumentException();
                    }
                    rule.append("BYMONTHDAY=" + monthDays + ";");
                }
            } else {
                throw new IllegalArgumentException("frequency is only supported for daily, weekly, and monthly.");
            }
        }
        return rule.toString();
    }

    private String translateWeekIntegersToDays(String expression) throws IllegalArgumentException {
        StringBuilder daysResult = new StringBuilder();
        boolean[] daysAlreadyCounted = new boolean[7];
        String[] days = expression.split(",");
        int dayNumber;
        for (final String day : days) {
            dayNumber = Integer.parseInt(day);
            dayNumber = dayNumber == 7 ? 0 : dayNumber;
            if (!daysAlreadyCounted[dayNumber]) {
                daysResult.append(dayNumberToDayOfWeekString(dayNumber) + ",");
                daysAlreadyCounted[dayNumber] = true;
            }
        }
        if (days.length == 0) {
            throw new IllegalArgumentException("must have at least 1 day of the week if specifying repeating weekly");
        }
        daysResult.deleteCharAt(daysResult.length() - 1);
        return daysResult.toString();
    }

    private String translateMonthIntegersToDays(String expression) throws IllegalArgumentException {
        StringBuilder daysResult = new StringBuilder();
        boolean[] daysAlreadyCounted = new boolean[2 * MAX_NUMBER_DAYS_IN_MONTH + 1]; //for -31 to 31
        String[] days = expression.split(",");
        int dayNumber;
        for (final String day : days) {
            dayNumber = Integer.parseInt(day);
            if (!daysAlreadyCounted[dayNumber + MAX_NUMBER_DAYS_IN_MONTH]) {
                daysResult.append(dayNumberToDayOfMonthString(dayNumber) + ",");
                daysAlreadyCounted[dayNumber + MAX_NUMBER_DAYS_IN_MONTH] = true;
            }
        }
        if (days.length == 0) {
            throw new IllegalArgumentException("must have at least 1 day of the month if specifying repeating weekly");
        }
        daysResult.deleteCharAt(daysResult.length() - 1);
        return daysResult.toString();
    }

    private String dayNumberToDayOfWeekString(int number) throws IllegalArgumentException {
        String dayOfWeek;
        switch (number) {
            case 0:
                dayOfWeek = "SU";
                break;
            case 1:
                dayOfWeek = "MO";
                break;
            case 2:
                dayOfWeek = "TU";
                break;
            case 3:
                dayOfWeek = "WE";
                break;
            case 4:
                dayOfWeek = "TH";
                break;
            case 5:
                dayOfWeek = "FR";
                break;
            case 6:
                dayOfWeek = "SA";
                break;
            default:
                throw new IllegalArgumentException("invalid day of week " + number);
        }
        return dayOfWeek;
    }

    private String dayNumberToDayOfMonthString(int number) throws IllegalArgumentException {
        String dayOfMonth;
        // https://android.googlesource.com/platform/frameworks/opt/calendar/+/504844526f1b7afec048c6d2976ffb332670d5ba/src/com/android/calendarcommon2/EventRecurrence.java
        if (number != 0 && number >= -MAX_NUMBER_DAYS_IN_MONTH && number <= MAX_NUMBER_DAYS_IN_MONTH) {
            dayOfMonth = "" + number;
        } else {
            throw new IllegalArgumentException("invalid day of month " + number);
        }
        return dayOfMonth;
    }

    /**
     * Downloads an image from a remote URL and stores it in the user's photo gallery.
     *
     * This runs on the background thread, creates the appropriate photo gallery directory if not
     * present, and uses the mime-type to append a file extension if necessary.
     *
     * The DownloadImageAsyncTaskListener parameter is notified of task success or failure.
     */
    @VisibleForTesting
    static class DownloadImageAsyncTask extends AsyncTask<String, Void, Boolean> {
        interface DownloadImageAsyncTaskListener {
            void onSuccess();
            void onFailure();
        }

        private final Context mContext;
        private final DownloadImageAsyncTaskListener mListener;

        public DownloadImageAsyncTask(@NonNull final Context context,
                @NonNull final DownloadImageAsyncTaskListener listener) {
            super();
            mContext = context.getApplicationContext();
            mListener = listener;
        }

        @Override
        protected Boolean doInBackground(@NonNull String[] params) {
            if (params == null || params.length == 0 || params[0] == null) {
                return false;
            }

            final File pictureStoragePath = getPictureStoragePath();

            //noinspection ResultOfMethodCallIgnored
            pictureStoragePath.mkdirs();

            final String uriString = params[0];
            URI uri = URI.create(uriString);

            InputStream pictureInputStream = null;
            OutputStream pictureOutputStream = null;
            try {
                final HttpURLConnection urlConnection =
                        MoPubHttpUrlConnection.getHttpUrlConnection(uriString);
                pictureInputStream = new BufferedInputStream(urlConnection.getInputStream());

                final String redirectLocation = urlConnection.getHeaderField(LOCATION.getKey());
                if (!TextUtils.isEmpty(redirectLocation)) {
                    uri = URI.create(redirectLocation);
                }

                final String pictureFileName =
                        getFileNameForUriAndHeaders(uri, urlConnection.getHeaderFields());
                final File pictureFile = new File(pictureStoragePath, pictureFileName);
                pictureOutputStream = new FileOutputStream(pictureFile);
                Streams.copyContent(pictureInputStream, pictureOutputStream);

                final String pictureFileFullPath = pictureFile.toString();
                loadPictureIntoGalleryApp(pictureFileFullPath);

                return true;
            } catch (Exception e) {
                return false;
            } finally {
                Streams.closeStream(pictureInputStream);
                Streams.closeStream(pictureOutputStream);
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success != null && success) {
                mListener.onSuccess();
            } else {
                mListener.onFailure();
            }
        }

        @Nullable
        private String getFileNameForUriAndHeaders(@NonNull final URI uri,
                @Nullable final Map<String, List<String>> headers) {
            Preconditions.checkNotNull(uri);
            final String path = uri.getPath();

            if (path == null || headers == null) {
                return null;
            }
            String filename = new File(path).getName();

            final List<String> mimeTypeHeaders = headers.get(ResponseHeader.CONTENT_TYPE.getKey());
            if (mimeTypeHeaders == null || mimeTypeHeaders.isEmpty()
                    || mimeTypeHeaders.get(0) == null) {
                return filename;
            }

            // Capture the first MIME_TYPE_HEADER (e.g. "text/plain; image/jpeg; image/gif") and
            // parse out supported Content-Types (e.g. {"text/plain", "image/jpeg", "image/gif"}).
            // If any of the Content-Types are of type "image", use the extension matching the first
            // associated content subtype: add this extension to the filename if it does not already
            // include it.
            final String[] fields = mimeTypeHeaders.get(0).split(";");
            for (final String field : fields) {
                if (field.contains("image/")) {
                    final String extension = "." + field.split("/")[1];
                    if (!filename.endsWith(extension)) {
                        filename += extension;
                    }
                    break;
                }
            }

            return filename;
        }

        private File getPictureStoragePath() {
            return new File(Environment.getExternalStorageDirectory(), "Pictures");
        }

        private void loadPictureIntoGalleryApp(final String filename) {
            MoPubMediaScannerConnectionClient mediaScannerConnectionClient =
                    new MoPubMediaScannerConnectionClient(filename, null);
            final MediaScannerConnection mediaScannerConnection =
                    new MediaScannerConnection(mContext, mediaScannerConnectionClient);
            mediaScannerConnectionClient.setMediaScannerConnection(mediaScannerConnection);
            mediaScannerConnection.connect();
        }

        @VisibleForTesting
        @Deprecated
        DownloadImageAsyncTaskListener getListener() {
            return mListener;
        }
    }

    private static class MoPubMediaScannerConnectionClient
            implements MediaScannerConnection.MediaScannerConnectionClient {
        private final String mFilename;
        private final String mMimeType;
        private MediaScannerConnection mMediaScannerConnection;

        private MoPubMediaScannerConnectionClient(String filename, String mimeType) {
            mFilename = filename;
            mMimeType = mimeType;
        }

        private void setMediaScannerConnection(MediaScannerConnection connection) {
            mMediaScannerConnection = connection;
        }

        @Override
        public void onMediaScannerConnected() {
            if (mMediaScannerConnection != null) {
                mMediaScannerConnection.scanFile(mFilename, mMimeType);
            }
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            if (mMediaScannerConnection != null) {
                mMediaScannerConnection.disconnect();
            }
        }
    }
}
