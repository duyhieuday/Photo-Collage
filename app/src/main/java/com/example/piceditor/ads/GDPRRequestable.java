package com.example.piceditor.ads;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.piceditor.BuildConfig;
import com.example.piceditor.WeatherApplication;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

public class GDPRRequestable {
    public ConsentInformation consentInformation;
    public static ConsentForm consentForm;
    private Activity context;
    static GDPRRequestable gdprRequestable;
    public static String YOUR_TEST_DEVICE_ID = "8734B689F9349F63CC0C0925E92268EE";

    public GDPRRequestable(Activity context) {
        this.context = context;
    }

    public static GDPRRequestable getGdprRequestable(Activity activity){
        if(gdprRequestable == null){
            return gdprRequestable = new GDPRRequestable(activity);
        }
        else return gdprRequestable;
    }

    public interface RequestGDPRCompleted {
        void onRequestGDPRCompleted(FormError formError);
    }

    private RequestGDPRCompleted onRequestGDPRCompleted;

    public void setOnRequestGDPRCompleted(RequestGDPRCompleted onRequestGDPRCompleted) {
        this.onRequestGDPRCompleted = onRequestGDPRCompleted;
    }

    public void requestGDPR() {
        ConsentDebugSettings.Builder consentDebugSettingsBuilder = new ConsentDebugSettings
                .Builder(context);
        if (BuildConfig.DEBUG) {
            consentDebugSettingsBuilder
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(YOUR_TEST_DEVICE_ID);
        }

        ConsentDebugSettings consentDebugSettings = consentDebugSettingsBuilder.build();

        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setConsentDebugSettings(consentDebugSettings)
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(context);
        consentInformation.requestConsentInfoUpdate(
                context,
                params,
                new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                    @Override
                    public void onConsentInfoUpdateSuccess() {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        } else {
                            onRequestGDPRCompleted.onRequestGDPRCompleted(null);
                        }
                    }
                },
                new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                    @Override
                    public void onConsentInfoUpdateFailure(@NonNull FormError formError) {
                       onRequestGDPRCompleted.onRequestGDPRCompleted(formError);
                    }
                }
        );
    }

    private void loadForm() {
        UserMessagingPlatform.loadConsentForm(context, new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
            @Override
            public void onConsentFormLoadSuccess(@NonNull ConsentForm consentForm) {
                GDPRRequestable.consentForm = consentForm;
                if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                    WeatherApplication.trackingEvent("show_GDPR");

                    GDPRRequestable.consentForm.show(context, new ConsentForm.OnConsentFormDismissedListener() {
                        @Override
                        public void onConsentFormDismissed(@Nullable FormError formError) {
                            if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED) {
                               onRequestGDPRCompleted.onRequestGDPRCompleted(null);
                                WeatherApplication.trackingEvent("show_GDPR_OK");
                            }
                          //  loadForm();
                        }
                    });
                }else if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED) {
                    onRequestGDPRCompleted.onRequestGDPRCompleted(null);
                    WeatherApplication.trackingEvent("show_GDPR_OK2");
                }
            }
        }, new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
            @Override
            public void onConsentFormLoadFailure(@NonNull FormError formError) {
              onRequestGDPRCompleted.onRequestGDPRCompleted(formError);
            }
        });
    }
}

