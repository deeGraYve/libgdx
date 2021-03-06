/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.badlogic.gdx.backends.angle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.anglejni.ESLoop;
import com.badlogic.anglejni.ESUtil;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.GdxNativesLoader;

public class AngleApplication implements Application, ESLoop {
	AngleGraphics graphics;
	AngleAudio audio;
	AngleInput input;
	AngleFiles files;
	ESUtil utils;
	ApplicationListener listener;
	List<Runnable> runnables = new ArrayList<Runnable>();
	boolean created = false;

	public AngleApplication (final ApplicationListener listener, final String title, final int width, final int height,
		final boolean fullscreen) {
		new Thread(new Runnable() {
			public void run () {
				GdxNativesLoader.load();

				AngleApplication.this.listener = listener;
				utils = new ESUtil(title, width, height, ESUtil.ES_WINDOW_DEPTH | (fullscreen ? ESUtil.ES_WINDOW_FULLSCREEN : 0));
				graphics = new AngleGraphics(width, height);
				audio = new AngleAudio();
				input = new AngleInput();
				files = new AngleFiles();

				Gdx.app = AngleApplication.this;
				Gdx.graphics = graphics;
				Gdx.audio = audio;
				Gdx.input = input;
				Gdx.files = files;
				Gdx.gl = graphics.getGL20();
				Gdx.gl20 = graphics.getGL20();
				utils.run(AngleApplication.this);
			}
		}).run();
	}

	@Override public Graphics getGraphics () {
		return graphics;
	}

	@Override public Audio getAudio () {
		return audio;
	}

	@Override public Input getInput () {
		return input;
	}

	@Override public Files getFiles () {
		return files;
	}

	@Override public void log (String tag, String message) {
		System.out.println(tag + ": " + message);
	}

	@Override public ApplicationType getType () {
		return ApplicationType.Desktop;
	}

	@Override public int getVersion () {
		return 0;
	}

	@Override public long getJavaHeap () {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	@Override public long getNativeHeap () {
		return getJavaHeap();
	}

	@Override public void onKey (int action, int key, int keyCode) {
		input.registerKeyEvent(action, key, keyCode);
	}

	@Override public void onMouse (int action, int x, int y, int button) {
		input.registerMouseEvent(action, x, y, button);
	}

	@Override public void quit () {
		listener.pause();
		listener.dispose();
	}

	@Override public void render () {
		graphics.updateTime();
		if (!created) {
			listener.create();			
			created = true;
		}
		synchronized (runnables) {
			for(int i = 0; i < runnables.size(); i++) {
				runnables.get(i).run();
			}
			runnables.clear();
		}
		input.processEvents();
		listener.render();
		input.justTouched = false;
	}

	@Override public void resize (int width, int height) {
		graphics.width = width;
		graphics.height = height;
		if (!created) listener.resize(width, height);
	}

	Map<String, Preferences> preferences = new HashMap<String, Preferences>();
	@Override public Preferences getPreferences (String name) {
		if(preferences.containsKey(name)) {
			return preferences.get(name);
		} else {
			Preferences prefs = new AnglePreferences(name);
			preferences.put(name, prefs);
			return prefs;
		}
	}

	@Override public void postRunnable (Runnable runnable) {
		synchronized(runnables) {
			runnables.add(runnable);
		}	
	}
	
	@Override public void log (String tag, String message, Exception exception) {
		System.out.println(tag + ": " + message);
		exception.printStackTrace();
	}
}
