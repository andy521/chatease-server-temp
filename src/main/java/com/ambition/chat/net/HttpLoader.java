package com.ambition.chat.net;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.ambition.chat.events.ErrorEvent;
import com.ambition.chat.events.Event;
import com.ambition.chat.events.EventDispathcer;
import com.ambition.chat.events.LoaderEvent;
import com.ambition.chat.utils.Utils;

public class HttpLoader extends EventDispathcer {

	private static final Logger logger;
	
	private String url = null;
	private JSONObject params = null;
	private String method = "get";
	
	private CloseableHttpAsyncClient client;
	
	static {
		logger = LogManager.getLogger(HttpLoader.class);
	}
	
	public HttpLoader() {
		this.client = HttpAsyncClients.createDefault();
	}
	
	public void load(String url, JSONObject params, String method) {
		this.url = url;
		this.params = params;
		this.method = method != null && method.equalsIgnoreCase("post") ? "post" : "get";
		
		this.client.start();
		this.client.execute(this.getRequest(), new FutureCallback<HttpResponse>() {
			
			@Override
			public void failed(Exception e) {
				logger.error("HttpLoader failed. Error: " + e.toString());
				HttpLoader.this.dispatchEvent(new ErrorEvent(HttpLoader.this, Event.ERROR));
				HttpLoader.this.close();
			}
			
			@Override
			public void completed(HttpResponse response) {
				logger.warn("HttpLoader completed.");
				HttpLoader.this.dispatchEvent(new LoaderEvent(HttpLoader.this, Event.COMPLETE, response));
				HttpLoader.this.close();
			}
			
			@Override
			public void cancelled() {
				logger.warn("HttpLoader cancelled.");
				HttpLoader.this.dispatchEvent(new LoaderEvent(HttpLoader.this, LoaderEvent.CANCEL));
				HttpLoader.this.close();
			}
		});
	}
	
	public void syncLoad(String url, JSONObject params, String method) {
		this.url = url;
		this.params = params;
		this.method = method.equalsIgnoreCase("post") ? "post" : "get";
		
		Future<HttpResponse> future = null;
		HttpResponse response = null;
		try {
			client.start();
			future = client.execute(this.getRequest(), null);
			response = future.get();
		} catch (Exception e) {
			logger.error("HttpLoader failed. Error: " + e.toString());
			this.dispatchEvent(new ErrorEvent(this, Event.ERROR));
			this.close();
			return;
		}
		
		StatusLine statusline = response.getStatusLine();
		logger.warn("Got user info response: " + statusline.toString());
		
		int statuscode = statusline.getStatusCode();
		switch (statuscode) {
			case 200:
				logger.warn("HttpLoader completed.");
				this.dispatchEvent(new LoaderEvent(this, Event.COMPLETE, response));
				this.close();
				break;
			/*case 301:
			case 302:
				if (response.containsHeader("location")) {
					this.url = response.getHeaders("location").toString();
					this.syncLoad(this.url, this.params, this.method);
				}
				break;*/
			default:
				logger.error("HttpLoader failed. Error: " + statuscode + " " + statusline.getReasonPhrase());
				this.dispatchEvent(new ErrorEvent(this, Event.ERROR));
				this.close();
				break;
		}
	}
	
	private HttpUriRequest getRequest() {
		HttpUriRequest request;
		if (this.method.equals("post")) {
			request = new HttpPost(this.url);
			request.addHeader("Content-type", "application/json; charset=utf-8");
			request.setHeader("Accept", "application/json");
			((HttpPost) request).setEntity(new StringEntity(this.params.toString(), Charset.forName("UTF-8")));
		} else {
			String paramstr = "";
			if (this.params != null) {
				Iterator<String> it = this.params.keys();
				while (it.hasNext()) {
					String key = it.next();
					Object val = this.params.get(key);
					paramstr += Utils.addQuery(this.url, key + "=" + val.toString());
				}
			}
			request = new HttpGet(Utils.addQuery(this.url, paramstr));
		}
		return request;
	}
	
	private void close() {
		try {
			this.client.close();
		} catch (IOException e) {
			// Ignore this.
		}
	}
}
