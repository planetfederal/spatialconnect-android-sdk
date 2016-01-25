package com.boundlessgeo.spatialconnect.jsbridge;

import android.app.Activity;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * @see <a href="https://github.com/boundlessgeo/WebViewJavascriptBridge/blob/master/Android/WebViewJavascriptBridge/src/com/fangjian/WebViewJavascriptBridge.java">https://github.com/boundlessgeo/WebViewJavascriptBridge/blob/master/Android/WebViewJavascriptBridge/src/com/fangjian/WebViewJavascriptBridge.java</a>
 */
public class WebViewJavascriptBridge implements Serializable {

    WebView mWebView;
    Activity mContext;
    WVJBHandler _messageHandler;
    Map<String, WVJBHandler> _messageHandlers;
    Map<String, WVJBResponseCallback> _responseCallbacks;
    long _uniqueId;
    private static ObjectMapper MAPPER = new ObjectMapper();

    public WebViewJavascriptBridge(Activity context, WebView webview, WVJBHandler handler) {
        this.mContext = context;
        this.mWebView = webview;
        this._messageHandler = handler;
        _messageHandlers = new HashMap<String, WVJBHandler>();
        _responseCallbacks = new HashMap<String, WVJBResponseCallback>();
        _uniqueId = 0;
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(this, "_WebViewJavascriptBridge");
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.setWebChromeClient(new MyWebChromeClient());     //optional, for show console and alert
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String convertStreamToString(java.io.InputStream is) {
        String s = "";
        try {
            Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
            if (scanner.hasNext()) s = scanner.next();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView webView, String url) {
            Log.d("test", "onPageFinished");
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage cm) {
            Log.d("test", cm.message()
                            + " line:" + cm.lineNumber()
            );
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            // if don't cancel the alert, webview after onJsAlert not responding taps
            // you can check this :
            // http://stackoverflow.com/questions/15892644/android-webview-after-onjsalert-not-responding-taps
            result.cancel();
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    public interface WVJBHandler {
        public void handle(String data, WVJBResponseCallback jsCallback);
    }

    public interface WVJBResponseCallback {
        public void callback(String data);
    }

    public void registerHandler(String handlerName, WVJBHandler handler) {
        _messageHandlers.put(handlerName, handler);
    }

    private class CallbackJs implements WVJBResponseCallback {
        private final String callbackIdJs;

        public CallbackJs(String callbackIdJs) {
            this.callbackIdJs = callbackIdJs;
        }

        @Override
        public void callback(String data) {
            _callbackJs(callbackIdJs, data);
        }
    }


    private void _callbackJs(String callbackIdJs, String data) {
        MessagePayload messagePayload = new MessagePayload();
        messagePayload.setData(data);
        messagePayload.setResponseId(callbackIdJs);
        _dispatchMessage(messagePayload);
    }

    @JavascriptInterface
    public void _handleMessageFromJs(final String data, String responseId,
                                     String responseData, String callbackId, String handlerName) {
        if (null != responseId) {
            WVJBResponseCallback responseCallback = _responseCallbacks.get(responseId);
            responseCallback.callback(responseData);
            _responseCallbacks.remove(responseId);
        } else {
            final WVJBResponseCallback responseCallback = (null != callbackId) ? new CallbackJs(callbackId) : null;
            final WVJBHandler handler;
            if (null != handlerName) {
                handler = _messageHandlers.get(handlerName);
                if (null == handler) {
                    Log.e("test", "WVJB Warning: No handle" +
                            "" +
                            "r for " + handlerName);
                    return;

                }
            } else {
                handler = _messageHandler;
            }
            try {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handler.handle(data, responseCallback);
                    }
                });
            } catch (Exception exception) {
                Log.e("test", "WebViewJavascriptBridge: WARNING: java handler threw. " + exception.getMessage());
            }
        }
    }

    public void send(String data) {
        send(data, null);
    }

    public void send(String data, WVJBResponseCallback responseCallback) {
        _sendData(data, responseCallback, null);
    }

    private void _sendData(String data, WVJBResponseCallback responseCallback, String handlerName) {
        MessagePayload messagePayload = new MessagePayload();
        messagePayload.setData(data);
        messagePayload.setHandlerName(handlerName);
        if (null != responseCallback) {
            String callbackId = "java_cb_" + (++_uniqueId);
            _responseCallbacks.put(callbackId, responseCallback);
            messagePayload.setCallbackId(callbackId);
        }
        _dispatchMessage(messagePayload);

    }

    private void _dispatchMessage(MessagePayload message) {
        String messageJSON = null;
        try {
            messageJSON = MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        Log.d("test", "sending:" + messageJSON);
        final String javascriptCommand =
                String.format(
                        "javascript:WebViewJavascriptBridge._handleMessageFromJava('%s');",
                        doubleEscapeString(messageJSON)
                );
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(javascriptCommand);
            }
        });
    }


    public void callHandler(String handlerName) {
        callHandler(handlerName, null, null);
    }

    public void callHandler(String handlerName, String data) {
        callHandler(handlerName, data, null);
    }

    public void callHandler(String handlerName, String data, WVJBResponseCallback responseCallback) {
        _sendData(data, responseCallback, handlerName);
    }

    /*
      * you must escape the char \ and  char ", or you will not recevie a correct json object in
      * your javascript which will cause a exception in chrome.
      *
      * please check this and you will know why.
      * http://stackoverflow.com/questions/5569794/escape-nsstring-for-javascript-input
      * http://www.json.org/
    */
    private String doubleEscapeString(String javascript) {
        String result;
        result = javascript.replace("\\", "\\\\");
        result = result.replace("\"", "\\\"");
        result = result.replace("\'", "\\\'");
        result = result.replace("\n", "\\n");
        result = result.replace("\r", "\\r");
        result = result.replace("\f", "\\f");
        return result;
    }

    class MessagePayload {
        private JsonNode data;
        private String handlerName;
        private String callbackId;
        private String responseId;

        public MessagePayload() {
        }

        public JsonNode getData() {
            return data;
        }

        /**
         * Takes a string of json data and transforms it into a JsonNode.
         *
         * @param data
         */
        public void setData(String data) {
            try {
                this.data = MAPPER.readTree(data);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("The string was not valid json: " + data);
            }
        }

        public String getHandlerName() {
            return handlerName;
        }

        public void setHandlerName(String handlerName) {
            this.handlerName = handlerName;
        }

        public String getCallbackId() {
            return callbackId;
        }

        public void setCallbackId(String callbackId) {
            this.callbackId = callbackId;
        }

        public String getResponseId() {
            return responseId;
        }

        public void setResponseId(String responseId) {
            this.responseId = responseId;
        }
    }
}
