// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.flutter.plugins.googlemobileads;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoController.VideoLifecycleCallbacks;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAd.OnNativeAdLoadedListener;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Map;

import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugins.googlemobileads.GoogleMobileAdsPlugin.NativeAdFactory;
import io.flutter.plugins.googlemobileads.nativetemplates.FlutterNativeTemplateStyle;

/** A wrapper for {@link NativeAd}. */
class FlutterNativeAd extends FlutterAd {
  private static final String TAG = "FlutterNativeAd";

  @NonNull private final AdInstanceManager manager;
  @NonNull private final String adUnitId;
  @Nullable private final NativeAdFactory adFactory;
  @NonNull private final FlutterAdLoader flutterAdLoader;
  @Nullable private FlutterAdRequest request;
  @Nullable private FlutterAdManagerAdRequest adManagerRequest;
  @Nullable private Map<String, Object> customOptions;
  @Nullable private NativeAdView nativeAdView;
  @Nullable private NativeAd nativeAd;
  @Nullable private final FlutterNativeAdOptions nativeAdOptions;
  @Nullable private final FlutterNativeTemplateStyle nativeTemplateStyle;
  @Nullable private TemplateView templateView;
  @NonNull private final Context context;
  @NonNull private final VideoLifecycleCallbacksImpl videoLifecycleCallbacks =
      new VideoLifecycleCallbacksImpl();

  static class Builder {
    @Nullable private AdInstanceManager manager;
    @Nullable private String adUnitId;
    @Nullable private NativeAdFactory adFactory;
    @Nullable private FlutterAdRequest request;
    @Nullable private FlutterAdManagerAdRequest adManagerRequest;
    @Nullable private Map<String, Object> customOptions;
    @Nullable private Integer id;
    @Nullable private FlutterNativeAdOptions nativeAdOptions;
    @Nullable private FlutterAdLoader flutterAdLoader;
    @Nullable private FlutterNativeTemplateStyle nativeTemplateStyle;
    @NonNull private final Context context;

    Builder(Context context) {
      this.context = context;
    }

    @CanIgnoreReturnValue
    public Builder setAdFactory(@NonNull NativeAdFactory adFactory) {
      this.adFactory = adFactory;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setId(int id) {
      this.id = id;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setCustomOptions(@Nullable Map<String, Object> customOptions) {
      this.customOptions = customOptions;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setManager(@NonNull AdInstanceManager manager) {
      this.manager = manager;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setAdUnitId(@NonNull String adUnitId) {
      this.adUnitId = adUnitId;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setRequest(@NonNull FlutterAdRequest request) {
      this.request = request;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setAdManagerRequest(@NonNull FlutterAdManagerAdRequest request) {
      this.adManagerRequest = request;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setNativeAdOptions(@Nullable FlutterNativeAdOptions nativeAdOptions) {
      this.nativeAdOptions = nativeAdOptions;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setFlutterAdLoader(@NonNull FlutterAdLoader flutterAdLoader) {
      this.flutterAdLoader = flutterAdLoader;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setNativeTemplateStyle(
        @Nullable FlutterNativeTemplateStyle nativeTemplateStyle) {
      this.nativeTemplateStyle = nativeTemplateStyle;
      return this;
    }

    FlutterNativeAd build() {
      if (manager == null) {
        throw new IllegalStateException("AdInstanceManager cannot be null.");
      } else if (adUnitId == null) {
        throw new IllegalStateException("AdUnitId cannot be null.");
      } else if (adFactory == null && nativeTemplateStyle == null) {
        throw new IllegalStateException("NativeAdFactory and nativeTemplateStyle cannot be null.");
      } else if (request == null && adManagerRequest == null) {
        throw new IllegalStateException("adRequest or addManagerRequest must be non-null.");
      }

      final FlutterNativeAd nativeAd;
      if (request == null) {
        nativeAd =
            new FlutterNativeAd(
                context,
                id,
                manager,
                adUnitId,
                adFactory,
                adManagerRequest,
                flutterAdLoader,
                customOptions,
                nativeAdOptions,
                nativeTemplateStyle);
      } else {
        nativeAd =
            new FlutterNativeAd(
                context,
                id,
                manager,
                adUnitId,
                adFactory,
                request,
                flutterAdLoader,
                customOptions,
                nativeAdOptions,
                nativeTemplateStyle);
      }
      return nativeAd;
    }
  }

  protected FlutterNativeAd(
      @NonNull Context context,
      int adId,
      @NonNull AdInstanceManager manager,
      @NonNull String adUnitId,
      @NonNull NativeAdFactory adFactory,
      @NonNull FlutterAdRequest request,
      @NonNull FlutterAdLoader flutterAdLoader,
      @Nullable Map<String, Object> customOptions,
      @Nullable FlutterNativeAdOptions nativeAdOptions,
      @Nullable FlutterNativeTemplateStyle nativeTemplateStyle) {
    super(adId);
    this.context = context;
    this.manager = manager;
    this.adUnitId = adUnitId;
    this.adFactory = adFactory;
    this.request = request;
    this.flutterAdLoader = flutterAdLoader;
    this.customOptions = customOptions;
    this.nativeAdOptions = nativeAdOptions;
    this.nativeTemplateStyle = nativeTemplateStyle;
  }

  protected FlutterNativeAd(
      @NonNull Context context,
      int adId,
      @NonNull AdInstanceManager manager,
      @NonNull String adUnitId,
      @NonNull NativeAdFactory adFactory,
      @NonNull FlutterAdManagerAdRequest adManagerRequest,
      @NonNull FlutterAdLoader flutterAdLoader,
      @Nullable Map<String, Object> customOptions,
      @Nullable FlutterNativeAdOptions nativeAdOptions,
      @Nullable FlutterNativeTemplateStyle nativeTemplateStyle) {
    super(adId);
    this.context = context;
    this.manager = manager;
    this.adUnitId = adUnitId;
    this.adFactory = adFactory;
    this.adManagerRequest = adManagerRequest;
    this.flutterAdLoader = flutterAdLoader;
    this.customOptions = customOptions;
    this.nativeAdOptions = nativeAdOptions;
    this.nativeTemplateStyle = nativeTemplateStyle;
  }

  boolean isCustomControlsEnabled() {
    // we check in an actual video controller, if the Ads is loaded and it is a video ads.
    final VideoController videoController = getVideoController();
    if (videoController != null) {
      return videoController.isCustomControlsEnabled();
    }
    // if the ads is still loading we can check if the ads request itself has the option enabled.
    return nativeAdOptions != null && nativeAdOptions.videoOptions != null
            && nativeAdOptions.videoOptions.customControlsRequested != null
        ? nativeAdOptions.videoOptions.customControlsRequested
        : false;
  }

  @Override
  void load() {
    final OnNativeAdLoadedListener loadedListener = new FlutterNativeAdLoadedListener(this);
    final AdListener adListener = new FlutterNativeAdListener(adId, manager);
    // Note we delegate loading the ad to FlutterAdLoader mainly for testing purposes.
    // As of 20.0.0 of GMA, mockito is unable to mock AdLoader.
    final NativeAdOptions options =
        this.nativeAdOptions == null
            ? new NativeAdOptions.Builder().build()
            : nativeAdOptions.asNativeAdOptions();
    if (request != null) {
      flutterAdLoader.loadNativeAd(
          adUnitId, loadedListener, options, adListener, request.asAdRequest(adUnitId));
    } else if (adManagerRequest != null) {
      AdManagerAdRequest adManagerAdRequest = adManagerRequest.asAdManagerAdRequest(adUnitId);
      flutterAdLoader.loadAdManagerNativeAd(
          adUnitId, loadedListener, options, adListener, adManagerAdRequest);
    } else {
      Log.e(TAG, "A null or invalid ad request was provided.");
    }
  }

  @Override
  @Nullable
  public PlatformView getPlatformView() {
    if (nativeAdView != null) {
      return new FlutterPlatformView(nativeAdView);
    } else if (templateView != null) {
      return new FlutterPlatformView(templateView);
    }
    return null;
  }

  void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
    if (nativeTemplateStyle != null) {
      templateView = nativeTemplateStyle.asTemplateView(context);
      templateView.setNativeAd(nativeAd);
    } else {
      nativeAdView = adFactory.createNativeAd(nativeAd, customOptions);
    }
    nativeAd.setOnPaidEventListener(new FlutterPaidEventListener(manager, this));
    this.nativeAd = nativeAd;
    manager.onAdLoaded(adId, nativeAd.getResponseInfo());
    @Nullable final VideoController videoController = getVideoController();
    if (videoController != null) {
      videoController.setVideoLifecycleCallbacks(videoLifecycleCallbacks);
    }
  }

  /**
   * Returns whether the ad has video content.
   */
  boolean hasVideoContent() {
    if (nativeAd == null) {
      return false;
    }
    final MediaContent mediaContent = nativeAd.getMediaContent();
    if (mediaContent == null) {
      return false;
    }
    return mediaContent.hasVideoContent();

  }

  /**
   * Returns the {@link VideoController} associated with this ad. This is used to control video
   * playback.
   * @return the video controller associated with this ad, if it is not a video ad, then it returns `null`.
   */
  @Nullable
  VideoController getVideoController() {
    if (nativeAd == null) {
      return null;
    }
    final MediaContent mediaContent = nativeAd.getMediaContent();
    if (mediaContent == null) {
      return null;
    }
    return mediaContent.getVideoController();
  }

  @Override
  void dispose() {
    if (nativeAdView != null) {
      nativeAdView.destroy();
      nativeAdView = null;
    }
    if (templateView != null) {
      templateView.destroyNativeAd();
      templateView = null;
    }
    final VideoController videoController = getVideoController();
    if (videoController != null) {
      videoController.setVideoLifecycleCallbacks(null);
    }
    nativeAd = null;
  }

  private class VideoLifecycleCallbacksImpl extends VideoLifecycleCallbacks {
    @Override
    public void onVideoStart() {
      manager.onNativeAdStartVideo(FlutterNativeAd.this);
    }

    @Override
    public void onVideoPlay() {
      manager.onNativeAdPlayVideo(FlutterNativeAd.this);
    }

    @Override
    public void onVideoPause() {
      manager.onNativeAdPauseVideo(FlutterNativeAd.this);
    }

    @Override
    public void onVideoEnd() {
      manager.onNativeAdEndVideo(FlutterNativeAd.this);
    }

    @Override
    public void onVideoMute(final boolean isMuted) {
      if (isMuted) {
        manager.onNativeAdMuteVideo(FlutterNativeAd.this);
      } else {
        manager.onNativeAdUnMuteVideo(FlutterNativeAd.this);
      }

    }
  }

  enum VideoLifecycleEvent {
    VIDEO_START("videoStart"),
    VIDEO_PLAY("videoPlay"),
    VIDEO_PAUSE("videoPause"),
    VIDEO_END("videoEnd"),
    VIDEO_MUTE("videoMute"),
    VIDEO_UNMUTE("videoUnmute");

    final String value;

    VideoLifecycleEvent(@NonNull final String value) {
      this.value = value;
    }
  }
}
