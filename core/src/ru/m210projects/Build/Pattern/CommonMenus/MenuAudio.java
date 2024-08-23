//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Pattern.CommonMenus;

import ru.m210projects.Build.Architecture.common.audio.AudioResampler;
import ru.m210projects.Build.Architecture.common.audio.BuildAudio;
import ru.m210projects.Build.Architecture.common.audio.MidiDevice;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.MenuItems.*;
import ru.m210projects.Build.settings.GameConfig;
import ru.m210projects.Build.Types.font.Font;

import java.util.List;
import java.util.stream.IntStream;

public abstract class MenuAudio extends BuildMenu {

    public interface AudioListener {
        default void PreDrvChange() {
        }

        default void PostDrvChange() {
        }

        default void SoundVolumeChange() {
        }

        default void SoundOn() {
        }

        default void SoundOff() {
        }

        default void MusicOn() {
        }

        default void MusicOff() {
        }
    }

    public MenuButton mApplyChanges;
    public MenuConteiner sSoundDrv;
    public MenuConteiner sMusicDrv;
    public MenuConteiner sResampler;
    public MenuSlider sSound;
    public MenuSlider sVoices;
    public MenuSwitch sSoundSwitch;
    public MenuSlider sMusic;
    public MenuSwitch sMusicSwitch;
    public MenuConteiner sMusicType;

    public int snddriver;
    public int middriver;
    public AudioResampler resampler;
    public int osnddriver;
    public int omiddriver;
    public AudioResampler oresampler;
    public int voices;
    public int ovoices;
    public int cdaudio;
    public int ocdaudio;

    private AudioListener listener;

    public MenuAudio(BuildGame app, int posx, int posy, int width, int menuHeight, int separatorHeight, Font menuItems) {
        super(app.pMenu);
        final GameConfig cfg = app.pCfg;

        addItem(getTitle(app, "Audio setup"), false);

        sSoundDrv = new MenuConteiner("Sound driver", menuItems, posx, posy += menuHeight, width, null, 0, (handler, pItem) -> {
            MenuConteiner item = (MenuConteiner) pItem;
            snddriver = item.num;
        }) {
            @Override
            public void open() {
                String[] names = app.pCfg.getAudioDevices().stream().map(BuildAudio::getName).toArray(String[]::new);
                BuildAudio currentAudio = app.pCfg.getAudio();

                if (this.list == null) {
                    this.list = new char[names.length][];
                    for (int i = 0; i < list.length; i++) {
                        this.list[i] = names[i].toCharArray();
                    }
                }

                int audioIndex = IntStream.range(0, names.length).filter(e -> names[e].equalsIgnoreCase(currentAudio.getName())).boxed().findAny().orElse(0);
                num = snddriver = osnddriver = audioIndex;
            }
        };

        sMusicDrv = new MenuConteiner("Midi driver", menuItems, posx, posy += menuHeight, width, null, 0, (h, pItem) -> middriver = ((MenuConteiner) pItem).num) {
            @Override
            public void open() {
                String[] names = app.pCfg.getMidiDevices().stream().map(MidiDevice::getName).toArray(String[]::new);
                MidiDevice currentDevice = app.pCfg.getMidiDevice();

                if (this.list == null) {
					this.list = new char[names.length][];
					for (int i = 0; i < list.length; i++) {
						this.list[i] = names[i].toCharArray();
					}
                }

                int midiIndex = IntStream.range(0, names.length).filter(e -> names[e].equalsIgnoreCase(currentDevice.getName())).boxed().findAny().orElse(0);
                num = middriver = omiddriver = midiIndex;
            }
        };

        sResampler = new MenuConteiner("Resampler", menuItems, posx, posy += menuHeight, width, null, 0, (handler, pItem) -> {
            MenuConteiner item = (MenuConteiner) pItem;
            List<AudioResampler> resamplers = cfg.getAudio().getResamplerList();
            if (resamplers.size() > item.num) {
                resampler = resamplers.get(item.num);
            }
        }) {
            @Override
            public void open() {
                if (this.list == null) {
                    updateSoftResamplers(this, cfg);
                }

                List<AudioResampler> resamplers = cfg.getAudio().getResamplerList();
                resampler = oresampler = cfg.getResampler();
                num = Gameutils.listIndexOf(resamplers, i -> resamplers.get(i).equals(resampler));
            }
        };

        posy += separatorHeight;
        int oposy = posy;
        posy += menuHeight;

        sSound = new MenuSlider(app.pSlider, "Sound volume", menuItems, posx, posy += menuHeight, width, (int) (cfg.getSoundVolume() * 256), 0, 256, 16, (handler, pItem) -> {
            MenuSlider slider = (MenuSlider) pItem;
            cfg.setSoundVolume(slider.value / 256.0f);
            if (listener != null) {
                listener.SoundVolumeChange();
            }
        }, false) {

            @Override
            public void draw(MenuHandler handler) {
                mCheckEnableItem(!cfg.isNoSound() /*&& audio.IsInited(Driver.Sound)*/);
                super.draw(handler);
            }
        };

        sVoices = new MenuSlider(app.pSlider, "Voices", menuItems, posx, posy += menuHeight, width, 0, 8, 256, 8, (handler, pItem) -> {
            MenuSlider slider = (MenuSlider) pItem;
            voices = slider.value;
        }, true) {
            @Override
            public void draw(MenuHandler handler) {
                mCheckEnableItem(!cfg.isNoSound() /*&& audio.IsInited(Driver.Sound)*/);
                super.draw(handler);
            }

            @Override
            public void open() {
                value = voices = ovoices = cfg.getMaxvoices();
            }
        };

        sSoundSwitch = new MenuSwitch("Sound", menuItems, posx, oposy + menuHeight, width, !cfg.isNoSound(), (handler, pItem) -> {
            MenuSwitch sw = (MenuSwitch) pItem;

            cfg.setNoSound(!sw.value);
            sSound.mCheckEnableItem(!cfg.isNoSound());
            sVoices.mCheckEnableItem(!cfg.isNoSound());
            if (listener != null) {
                if (sw.value) {
                    listener.SoundOn();
                } else {
                    listener.SoundOff();
                }
            }
        }, null, null);

        posy += separatorHeight;
        oposy = posy;
        posy += menuHeight;

        sMusic = new MenuSlider(app.pSlider, "Music volume", menuItems, posx, posy += menuHeight, width, (int) (cfg.getMusicVolume() * 256), 0, 256, 8, (handler, pItem) -> {
            MenuSlider slider = (MenuSlider) pItem;
            cfg.setMusicVolume(slider.value / 256.0f);
        }, false) {

            @Override
            public void draw(MenuHandler handler) {
                mCheckEnableItem(!cfg.isMuteMusic() /*&& audio.IsInited(Driver.Music)*/);
                super.draw(handler);
            }
        };

        sMusicSwitch = new MenuSwitch("Music", menuItems, posx, oposy + menuHeight, width, !cfg.isMuteMusic(), (handler, pItem) -> {
            MenuSwitch sw = (MenuSwitch) pItem;
            cfg.setMuteMusic(!sw.value);

            sMusic.mCheckEnableItem(!cfg.isMuteMusic());
            if (listener != null) {
                if (sw.value) {
                    listener.MusicOn();
                } else {
                    listener.MusicOff();
                }
            }
        }, null, null) {
            @Override
            public void draw(MenuHandler handler) {
                value = !cfg.isMuteMusic();
                super.draw(handler);
            }
        };

        sMusicType = new MenuConteiner("Music type", menuItems, posx, posy += menuHeight, width, null, 0, (handler, pItem) -> {
            MenuConteiner item = (MenuConteiner) pItem;
            cdaudio = item.num;
        }) {
            @Override
            public void open() {
                if (this.list == null) {
                    this.list = getMusicTypeList();
                }
                cdaudio = ocdaudio = num = cfg.getMusicType();
            }

            @Override
            public void draw(MenuHandler handler) {
                mCheckEnableItem(!cfg.isMuteMusic());
                super.draw(handler);
            }
        };

        MenuProc applyCallback = (handler, pItem) -> {
            if (snddriver != osnddriver || voices != ovoices || resampler != oresampler) {
                if (listener != null) {
                    listener.PreDrvChange();
                }

                if (snddriver != osnddriver) {
                    BuildAudio device = app.pCfg.getAudioDevices().get(snddriver);
                    app.pCfg.setAudioDriver(device.getAudioDriver());
                    int oldSelected = sSoundDrv.num;
                    sSoundDrv.open();
                    if (oldSelected == sSoundDrv.num) {
                        sSoundDrv.list[sSoundDrv.num] = app.pCfg.getAudio().getName().toCharArray();
                    }
                    updateSoftResamplers(sResampler, cfg);
                }

                if (voices != ovoices) {
                    cfg.setMaxvoices(voices);
                    ovoices = voices = cfg.getMaxvoices();
                }

                if (!resampler.equals(oresampler)) {
                    cfg.setResampler(resampler);
                    oresampler = resampler = cfg.getResampler();

                    List<AudioResampler> resamplers = cfg.getAudio().getResamplerList();
                    updateSoftResamplers(sResampler, cfg);
                    sResampler.num = Gameutils.listIndexOf(resamplers, i -> resamplers.get(i).equals(resampler));
                }
            } else {
                if (listener != null) {
                    listener.PreDrvChange();
                }

                if (middriver != omiddriver) {
                    MidiDevice device = app.pCfg.getMidiDevices().get(middriver);
                    if (app.pCfg.setMidiDevice(device)) {
                        omiddriver = middriver;
                    }
                    sMusicDrv.list[sMusicDrv.num] = app.pCfg.getMidiDevice().getName().toCharArray();
                }

                if (cdaudio != ocdaudio) {
                    cfg.setMusicType(cdaudio);
                    ocdaudio = cdaudio;
                }
            }
            if (listener != null) {
                listener.PostDrvChange();
            }
        };

        posy += 2 * separatorHeight;
        mApplyChanges = new MenuButton("Apply changes", menuItems, 0, posy, 320, 1, 0, null, -1, applyCallback, 0) {
            @Override
            public void draw(MenuHandler handler) {
                mCheckEnableItem(snddriver != osnddriver || middriver != omiddriver || resampler != oresampler || voices != ovoices || cdaudio != ocdaudio);
                super.draw(handler);
            }

            @Override
            public void mCheckEnableItem(boolean nEnable) {
                if (nEnable) {
                    flags = 3 | 4;
                } else {
                    flags = 3;
                }
            }
        };

        addItem(sSoundDrv, true);
        addItem(sMusicDrv, false);
        addItem(sResampler, false);
        addItem(sSoundSwitch, false);
        addItem(sSound, false);
        addItem(sVoices, false);
        addItem(sMusicSwitch, false);
        addItem(sMusic, false);
        addItem(sMusicType, false);
        addItem(mApplyChanges, false);
    }

    void updateSoftResamplers(MenuConteiner resamplersContainer, GameConfig cfg) {
        List<AudioResampler> resamplers = cfg.getAudio().getResamplerList();
        resamplersContainer.list = new char[resamplers.size()][];
        for (int i = 0; i < resamplers.size(); i++) {
            resamplersContainer.list[i] = resamplers.get(i).getName().toCharArray();
        }
        if (resamplersContainer.num >= resamplers.size()) {
            resamplersContainer.num = 0;
            resampler = oresampler = cfg.getResampler();
        }
    }

    protected char[][] getMusicTypeList() {
        char[][] list = new char[3][];
        list[0] = "midi".toCharArray();
        list[1] = "external".toCharArray();
        list[2] = "cd audio".toCharArray();

        return list;
    }

    protected void setListener(AudioListener listener) {
        this.listener = listener;
    }

    public abstract MenuTitle getTitle(BuildGame app, String text);

}
