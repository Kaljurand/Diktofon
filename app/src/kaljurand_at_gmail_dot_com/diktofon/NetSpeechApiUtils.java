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


import org.apache.http.client.ClientProtocolException;

import ee.ioc.phon.netspeechapi.AudioUploader;
import ee.ioc.phon.netspeechapi.TranscriptionDownloader;

/**
 * <p>This is just a layer on top of Net Speech API. We pass in the
 * Diktofon UA and convert any possible exceptions that come back.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class NetSpeechApiUtils {

	// Max file size supported by webtrans-server is 100MB.
	public static final int MAX_AUDIO_FILE_LENGTH = 100000000;

	// BUG: Set this from the resources
	public static final String USER_AGENT_DIKTOFON = "Diktofon/0.9.70";

	private static final int SAMPLE_RATE = 16000;

	private NetSpeechApiUtils() {}  // no instances allowed

	/**
	 * <p>Returns transcription that corresponds to the given token,
	 * returns null if the transcription is not available,
	 * throws an exception if a problem occurs.</p>
	 */
	public static String tokenToTrans(String token) throws TransException {
		//return mockTokenToTrans(token);
		TranscriptionDownloader td = new TranscriptionDownloader();
		td.setUserAgentComment(USER_AGENT_DIKTOFON);
		String trans = null;
		try {
			trans = td.download(token);
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
		if (! isLegalToken(token)) {
			throw new TransException("Illegal token: " + token);
		}
		return token;
	}


	public static String fileToToken(File file, String mimeType, String email) throws ClientProtocolException, IOException {
		//return mockFileToToken(file, mimeType, email);
		AudioUploader uploader = new AudioUploader(email);
		uploader.setUserAgentComment(USER_AGENT_DIKTOFON);
		return uploader.uploadFileUnderRandomName(file, mimeType, SAMPLE_RATE);
	}


	private static boolean isLegalToken(String token) {
		return ee.ioc.phon.netspeechapi.Utils.isLegalToken(token);
	}


	public static String mockFileToToken(File file, String mime, String email) throws ClientProtocolException, IOException {
		return "mock token";
	}

	public static String mockTokenToTrans(String token) throws TransException {
		return "This is a mock transcription!";
	}

}
