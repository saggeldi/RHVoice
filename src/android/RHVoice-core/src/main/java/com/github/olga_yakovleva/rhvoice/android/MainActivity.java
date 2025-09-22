/* Copyright (C) 2017, 2018, 2019  Olga Yakovleva <yakovleva.o.v@gmail.com> */

/* This program is free software: you can redistribute it and/or modify */
/* it under the terms of the GNU Lesser General Public License as published by */
/* the Free Software Foundation, either version 3 of the License, or */
/* (at your option) any later version. */

/* This program is distributed in the hope that it will be useful, */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the */
/* GNU Lesser General Public License for more details. */

/* You should have received a copy of the GNU Lesser General Public License */
/* along with this program.  If not, see <https://www.gnu.org/licenses/>. */

package com.github.olga_yakovleva.rhvoice.android;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.github.olga_yakovleva.rhvoice.compose.MainFragment;
import com.github.olga_yakovleva.rhvoice.compose.MainFragmentListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.Locale;

public final class MainActivity extends AppCompatActivity implements AvailableLanguagesFragment.Listener, AvailableVoicesFragment.Listener, ConfirmVoiceRemovalDialogFragment.Listener {
    private DataManager dm;
    private static final String PREFS_NAME = "RHVoicePrefs";
    private static final String PREF_FIRST_LAUNCH = "first_launch";

    TextToSpeech textToSpeech;
    EditText text;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    @Override
    protected void onCreate(Bundle state) {
        EdgeToEdge.enable(this);
        super.onCreate(state);
        dm = new DataManager();
        setContentView(R.layout.frame);
        Repository.get().getPackageDirectoryLiveData().observe(this, this::onPackageDirectory);
        
        // Check if this is the first time opening the app
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(PREF_FIRST_LAUNCH, true);
        
        if (state == null) {
            if (isFirstLaunch) {
                // First time opening - show language selection
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, new AvailableLanguagesFragment(), "languages")
                    .add(new PlayerFragment(), "player")
                    .commit();
                
                // Mark as no longer first launch
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(PREF_FIRST_LAUNCH, false);
                editor.apply();
            } else {
                // Not first time - show main fragment
                showMainFragment();
            }
        } else {
            // Restore state - check if main fragment exists, if not create it
            MainFragment existingFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.composeView);
            if (existingFragment == null) {
                showMainFragment();
            } else {
                // Fragment exists, just set the listener
                existingFragment.setListener(new MainFragmentListener() {
                    @Override
                    public void onSettingsClick() {
                        initBottomSheetBehavior();
                        openBottomSheet();
                    }
                });
            }
        }
        
        // textToSpeech.speak(text.getText().toString(),TextToSpeech.QUEUE_FLUSH,null);
    }

    private void onPackageDirectory(PackageDirectory dir) {
        dm.setPackageDirectory(dir);
        dm.scheduleSync(this, false);
    }
    
    private void showMainFragment() {
        MainFragment mainFragment = MainFragment.Companion.newInstance(new MainFragmentListener() {
            @Override
            public void onSettingsClick() {
                initBottomSheetBehavior();
                openBottomSheet();
            }
        });

        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.composeView, mainFragment, "main");
        transaction.commit();
    }

    public void onAccentSelected(VoiceAccent accent) {
        Bundle args = new Bundle();
        args.putString(AvailableVoicesFragment.ARG_LANGUAGE, accent.getLanguage().getId());
        args.putString(AvailableVoicesFragment.ARG_COUNTRY, accent.getTag().country3);
        args.putString(AvailableVoicesFragment.ARG_VARIANT, accent.getTag().variant);
        AvailableVoicesFragment frag = new AvailableVoicesFragment();
        frag.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, frag, "voices").addToBackStack(null).commit();
    }

    public void onVoiceSelected(VoicePack voice, boolean state) {
        if (state || !voice.isInstalled(this)) {
            voice.setEnabled(this, state);
            AvailableVoicesFragment frag = (AvailableVoicesFragment) (getSupportFragmentManager().findFragmentByTag("voices"));
            if (frag != null)
                frag.refresh(voice, VoiceViewChange.INSTALLED);
        } else {
            ConfirmVoiceRemovalDialogFragment.show(this, voice);
        }
    }

    public void onConfirmVoiceRemovalResponse(VoicePack voice, boolean response) {
        if (response) {
            voice.setEnabled(this, false);
            AvailableVoicesFragment frag = (AvailableVoicesFragment) (getSupportFragmentManager().findFragmentByTag("voices"));
            if (frag != null)
                frag.refresh(voice, VoiceViewChange.INSTALLED);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onStart() {
        super.onStart();
        Repository.get().refresh();
    }

    private void initBottomSheetBehavior() {
        View detailContainer = findViewById(R.id.settingsBottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(detailContainer);

        // Expanded by default
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setSkipCollapsed(false);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    finish();
                    overridePendingTransition(0, 0);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // no-op
            }
        });
    }

    // You can now use bottomSheetBehavior anywhere in MainActivity, e.g.:
    private void collapseBottomSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void openBottomSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

}
