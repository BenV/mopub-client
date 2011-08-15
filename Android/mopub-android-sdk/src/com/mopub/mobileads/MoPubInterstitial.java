/*
 * Copyright (c) 2011, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import java.util.HashMap;

import com.mopub.mobileads.MoPubView.OnAdFailedListener;
import com.mopub.mobileads.MoPubView.OnAdLoadedListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MoPubInterstitial {
    
    private MoPubInterstitialView mInterstitialView;
    private MoPubInterstitialListener mListener;
    private Activity mActivity;
    private String mAdUnitId;
    private Class mActivityClass;

    // TODO: Support prefetched native ads
    private boolean mPrefetchingAd;
    private static AdInfo sPrefetchedAd;


    public interface MoPubInterstitialListener {
        public void OnInterstitialLoaded();
        public void OnInterstitialFailed();
    }
    
    public class MoPubInterstitialView extends MoPubView {
        
        public MoPubInterstitialView(Context context) {
            super(context);
        }

        @Override
        protected void loadNativeSDK(HashMap<String, String> paramsHash) {
            MoPubInterstitial parent = MoPubInterstitial.this;
            String type = paramsHash.get("X-Adtype");

            if (type != null && type.equals("interstitial")) {
                Log.i("MoPub", "Loading native adapter for type: "+type);
                BaseInterstitialAdapter adapter =
                        BaseInterstitialAdapter.getAdapterForType(parent, type, paramsHash);
                if (adapter != null) {
                    adapter.loadInterstitial();
                    return;
                }
            }
            
            Log.i("MoPub", "Couldn't load native adapter. Trying next ad...");
            parent.interstitialFailed();
        }
        
        protected void trackImpression() {
            Log.d("MoPub", "Tracking impression for interstitial.");
            if (mAdView != null) mAdView.trackImpression();
        }
    }

    private static class AdInfo {
        private final String mAdUnitId;
        private final String mResponseString;

        public AdInfo(String adUnitId, String responseString){
            mAdUnitId = adUnitId;
            mResponseString = responseString;
        }

        public String getAdUnitId(){
            return(mAdUnitId);
        }

        public String getmResponseString(){
            return(mResponseString);
        }
    }
    
    public MoPubInterstitial(Activity activity, String id){
        this(activity, id, MoPubActivity.class);
    }

    public MoPubInterstitial(Activity activity, String id, Class<? extends MoPubActivity> activityClass) {
        mActivity = activity;
        mAdUnitId = id;
        mActivityClass = activityClass;
        
        mInterstitialView = new MoPubInterstitialView(mActivity);
        mInterstitialView.setAdUnitId(mAdUnitId);
        mInterstitialView.setOnAdLoadedListener(new OnAdLoadedListener() {
            public void OnAdLoaded(MoPubView m) {
                AdInfo adInfo = new AdInfo(mAdUnitId, mInterstitialView.getResponseString());
                if (mListener != null) {
                    mListener.OnInterstitialLoaded();
                }

                if(!mPrefetchingAd){
                    displayAd(adInfo);
                } else {
                    sPrefetchedAd = adInfo;
                    mPrefetchingAd = false;
                }
            }
        });
        mInterstitialView.setOnAdFailedListener(new OnAdFailedListener() {
            public void OnAdFailed(MoPubView m) {
                if (mListener != null) {
                    mListener.OnInterstitialFailed();
                }
                mPrefetchingAd = false;
            }
        });
    }
    
    public Activity getActivity() {
        return mActivity;
    }
    
    public void showAd() {
        mPrefetchingAd = false;
        mInterstitialView.loadAd();
    }

    public void prefetchAd() {
        if(sPrefetchedAd == null){
            mPrefetchingAd = true;
            mInterstitialView.loadAd();
        }
    }
    
    public boolean showPrefetchedAd() {
        AdInfo ad = sPrefetchedAd;
        if(ad != null){
            displayAd(ad);
            return(true);
        } else {
            return(false);
        }
    }

    public void setListener(MoPubInterstitialListener listener) {
        mListener = listener;
    }
    
    public MoPubInterstitialListener getListener() {
        return mListener;
    }
    
    protected void interstitialLoaded() {
        mInterstitialView.trackImpression();
        if (mListener != null) mListener.OnInterstitialLoaded();
    }

    protected void displayAd(AdInfo ad){
        if (ad != null && mActivity != null) {
            sPrefetchedAd = null;
            Intent i = new Intent(mActivity, mActivityClass);
            i.putExtra("com.mopub.mobileads.AdUnitId", ad.getAdUnitId());
            i.putExtra("com.mopub.mobileads.Source", ad.getmResponseString());
            mActivity.startActivity(i);
        }
    }
    
    protected void interstitialFailed() {
        mInterstitialView.loadFailUrl();
    }
    
    protected void interstitialClicked() {
        mInterstitialView.registerClick();
    }
    
    public void destroy() {
        mInterstitialView.destroy();
    }
}
