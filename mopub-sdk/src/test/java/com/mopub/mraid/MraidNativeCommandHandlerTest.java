// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.CalendarContract;
import androidx.annotation.NonNull;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.common.util.test.support.ShadowAsyncTasks;
import com.mopub.common.util.test.support.ShadowMoPubHttpUrlConnection;
import com.mopub.common.util.test.support.TestDrawables;
import com.mopub.mobileads.test.support.FileUtils;
import com.mopub.mraid.MraidNativeCommandHandler.DownloadImageAsyncTask;
import com.mopub.mraid.MraidNativeCommandHandler.DownloadImageAsyncTask.DownloadImageAsyncTaskListener;
import com.mopub.mraid.MraidNativeCommandHandler.MraidCommandFailureListener;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowToast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.os.Environment.MEDIA_MOUNTED;
import static com.mopub.mraid.MraidNativeCommandHandler.ANDROID_CALENDAR_CONTENT_TYPE;
import static java.io.File.separator;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(shadows = {ShadowAsyncTasks.class, ShadowMoPubHttpUrlConnection.class})
public class MraidNativeCommandHandlerTest {
    private static final String IMAGE_URI_VALUE = "file://tmp/expectedFile.jpg";
    private static final String REMOTE_IMAGE_URL = "https://www.mopub.com/expectedFile.jpg";
    private static final String FILE_PATH = "/tmp/expectedFile.jpg";
    private static final int TIME_TO_PAUSE_FOR_NETWORK = 300;
    private static final String FAKE_IMAGE_DATA = "imageFileData";
    //XXX: Robolectric or JUNIT doesn't support the correct suffix ZZZZZ in the parse pattern, so replacing xx:xx with xxxx for time.
    private static final String CALENDAR_START_TIME = "2013-08-14T20:00:00-0000";

    @Mock MraidCommandFailureListener mockMraidCommandFailureListener;
    @Mock DownloadImageAsyncTaskListener mockDownloadImageAsyncTaskListener;
    private MraidNativeCommandHandler subject;
    private Context context;
    private Map<String, String> params;

    private File expectedFile;
    private File pictureDirectory;
    private File fileWithoutExtension;

    @Before
    public void setUp() throws Exception {
        subject = new MraidNativeCommandHandler();
        context = Robolectric.buildActivity(Activity.class).create().get();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TestDrawables.EXPECTED_FILE.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, baos);
        FileUtils.writeBytesToFile(baos.toByteArray(), FILE_PATH);

        expectedFile = new File(Environment.getExternalStorageDirectory(), "Pictures" + separator + "expectedFile.jpg");
        pictureDirectory = new File(Environment.getExternalStorageDirectory(), "Pictures");
        fileWithoutExtension = new File(pictureDirectory, "file");

        // Mount external storage and grant necessary permissions
        ShadowEnvironment.setExternalStorageState(MEDIA_MOUNTED);
    }

    @After
    public void tearDown() {
        ShadowToast.reset();
        assertThat(new File(FILE_PATH).delete()).isTrue();
    }

    @Test
    public void isInlineVideoAvailable_whenViewsAreHardwareAccelerated_whenWindowIsHardwareAccelerated_shouldReturnTrue() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        View mockView = mock(View.class);
        when(mockView.isHardwareAccelerated()).thenReturn(true);
        when(mockView.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);

        assertThat(subject.isInlineVideoAvailable(activity, mockView)).isTrue();
    }

    @Test
    public void isInlineVideoAvailable_whenViewsAreHardwareAccelerated_whenWindowIsNotHardwareAccelerated_shouldReturnFalse() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();

        View mockView = mock(View.class);
        when(mockView.isHardwareAccelerated()).thenReturn(true);
        when(mockView.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);

        assertThat(subject.isInlineVideoAvailable(activity, mockView)).isFalse();
    }

    @Test
    public void isInlineVideoAvailable_whenViewsAreNotHardwareAccelerated_whenWindowIsHardwareAccelerated_shouldReturnTrue() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        View mockView = mock(View.class);
        when(mockView.isHardwareAccelerated()).thenReturn(false);
        when(mockView.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);

        assertThat(subject.isInlineVideoAvailable(activity, mockView)).isTrue();
    }

    @Test
    public void isInlineVideoAvailable_whenViewParentIsNotHardwareAccelerated_whenWindowIsHardwareAccelerated_shouldReturnTrue() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        // ViewParent
        LinearLayout mockLinearLayout = mock(LinearLayout.class);
        when(mockLinearLayout.isHardwareAccelerated()).thenReturn(false);
        when(mockLinearLayout.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);

        // View
        View mockView = mock(View.class);
        when(mockView.isHardwareAccelerated()).thenReturn(true);
        when(mockView.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);
        when(mockView.getParent()).thenReturn(mockLinearLayout);

        assertThat(subject.isInlineVideoAvailable(activity, mockView)).isTrue();
    }

    private static Context createMockContextWithSpecificIntentData(final String scheme, final String componentName, final String type, final String action) {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        resolveInfos.add(new ResolveInfo());

        when(context.getPackageManager()).thenReturn(packageManager);

        BaseMatcher intentWithSpecificData = new BaseMatcher() {
            // check that the specific intent has the special data, i.e. "tel:", or a component name, or string type, based on a particular data

            @Override
            public boolean matches(Object item) {
                if (item != null && item instanceof Intent ){
                    boolean result = action != null || type != null || componentName != null || scheme != null;
                    if (action != null) {
                        if (((Intent) item).getAction() != null) {
                            result = result && action.equals(((Intent) item).getAction());
                        }
                    }

                    if (type != null) {
                        if (((Intent) item).getType() != null) {
                            result = result && type.equals(((Intent) item).getType());
                        }
                    }

                    if (componentName != null) {
                        if (((Intent) item).getComponent() != null) {
                            result = result && componentName.equals(((Intent) item).getComponent().getClassName());
                        }
                    }

                    if (scheme != null) {
                        if (((Intent) item).getData() != null) {
                            result = result && scheme.equals(((Intent) item).getData().getScheme());
                        }
                    }
                    return result;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {

            }
        };

        // It is okay to query with specific intent or nothing, because by default, none of the query would normally any resolveInfo anyways
        when(packageManager.queryIntentActivities((Intent) argThat(intentWithSpecificData), eq(0))).thenReturn(resolveInfos);
        return context;
    }

    private Map<String, List<String>> createHeaders(@NonNull final Pair<String, String>... pairs) {
        final TreeMap<String, List<String>> headers = new TreeMap<String, List<String>>();
        for (final Pair<String, String> pair : pairs) {
            String key = pair.first;
            String value = pair.second;

            if (!headers.containsKey(key)) {
                headers.put(key, new ArrayList<String>());
            }
            headers.get(key).add(value);
        }

        return headers;
    }

    private void assertThatMimeTypeWasAddedCorrectly(String originalFileName, String contentType,
            String expectedFileName, String expectedExtension) throws Exception {
        expectedFile = new File(pictureDirectory, expectedFileName);

        ShadowMoPubHttpUrlConnection.addPendingResponse(200, FAKE_IMAGE_DATA,
                createHeaders(new Pair<String, String>(ResponseHeader.CONTENT_TYPE.getKey(), contentType)));

        final DownloadImageAsyncTask downloadImageAsyncTask =
                new DownloadImageAsyncTask(context, mockDownloadImageAsyncTaskListener);
        final Boolean result =
                downloadImageAsyncTask.doInBackground(new String[]{originalFileName});

        assertThat(result).isTrue();
        assertThat(expectedFile.exists()).isTrue();
        assertThat(expectedFile.getName()).endsWith(expectedExtension);
        assertThat(fileWithoutExtension.exists()).isFalse();
    }

    private void setupCalendarParams() {
        //we need mock Context so that we can validate that isCalendarAvailable() is true
        Context mockContext = createMockContextWithSpecificIntentData(null,
                null, ANDROID_CALENDAR_CONTENT_TYPE, "android.intent.action.INSERT");

        //but a mock context doesn't know how to startActivity(), so we stub it to use ShadowContext for starting activity
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (!(invocation.getArguments()[0] instanceof Intent)) {
                    throw new ClassCastException("For some reason you are not passing the calendar intent properly");
                }
                Context shadowContext = context;
                shadowContext.startActivity((Intent) invocation.getArguments()[0]);
                return null;
            }
        }).when(mockContext).startActivity(any(Intent.class));

        params.put("description", "Some Event");
        params.put("start", CALENDAR_START_TIME);
    }
}
