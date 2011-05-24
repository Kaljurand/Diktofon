/*
 * Copyright (C) 2011 Kaarel Kaljurand
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kaljurand_at_gmail_dot_com.diktofon;

import java.io.File;
import java.io.IOException;

import kaljurand_at_gmail_dot_com.estspeechapi.AudioUploader;
import kaljurand_at_gmail_dot_com.estspeechapi.TranscriptionDownloader;

import org.apache.http.client.ClientProtocolException;

/**
 * <p>This is just a layer on top of EstSpeechApi. We pass in the
 * Diktofon UA and convert any possible exceptions that come back.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class EstSpeechApiUtils {

	// Max file size supported by webtrans-server is 100MB.
	public static final int MAX_AUDIO_FILE_LENGTH = 100000000;

	// BUG: Set this from the resources
	public static final String USER_AGENT_DIKTOFON = "Diktofon/0.9.66";

	private EstSpeechApiUtils() {}  // no instances allowed

	/**
	 * Returns transcription that corresponds to the given token,
	 * returns null if the transcription is not available,
	 * throws an exception if a problem occurs.
	 */
	public static String tokenToTrans(String token) throws TransException {
		//return mockTokenToTrans(token);
		TranscriptionDownloader td = new TranscriptionDownloader(token);
		td.setUserAgentComment(USER_AGENT_DIKTOFON);
		String trans = null;
		try {
			trans = td.getTranscription();
		} catch (IOException e) {
			throw new TransException(e.getMessage());
		}
		return trans;
	}


	public static String noteToToken(File file, String mime, String email) throws TransException {
		String token = null;
		try {
			token = fileToToken(file, mime, email);
		} catch (ClientProtocolException e) {
			throw new TransException("Client protocol failed: " + e);
		} catch (IOException e) {
			throw new TransException("IO failed: " + e);
		}
		if (isIllegalToken(token)) {
			throw new TransException("Illegal token: " + token);
		}
		return token;
	}


	public static String fileToToken(File file, String mime, String email) throws ClientProtocolException, IOException {
		//return mockFileToToken(file, mime, email);
		AudioUploader uploader = new AudioUploader(email);
		uploader.setUserAgentComment(USER_AGENT_DIKTOFON);
		return uploader.upload(file, mime, 16000);
	}


	public static boolean isIllegalToken(String token) {
		return (token == null || token.length() > 50);
	}


	public static String mockFileToToken(File file, String mime, String email) throws ClientProtocolException, IOException {
		return "mock token";
	}

	public static String mockTokenToTrans(String token) throws TransException {
		return "This is a mock transcription!";
	}

}
