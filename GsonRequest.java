import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class GsonRequest<T, E> extends Request<T> {
    /**
     * Default charset for JSON request.
     */
    protected static final String PROTOCOL_CHARSET = "utf-8";

    /**
     * Content type for request.
     */
    private static final String PROTOCOL_CONTENT_TYPE =
            String.format("application/json; charset=%s", PROTOCOL_CHARSET);

    private final Gson gson = new Gson();

    private final Listener<T> listener;
    private final ErrorListener<E> errorListener;

    private final Class<T> clazz;
    private final Class<E> errorClazz;

    private final Map<String, String> headers;
    private final String mRequestBody;

    /**
     * Callback interface for delivering parsed responses.
     */
    public interface Listener<T> {
        /**
         * Called when a response is received.
         */
        void onResponse(T response);
    }

    public interface ErrorListener<E> {
        /**
         * Callback method that an error has been occurred with the
         * provided error code and optional user-readable message.
         */
        void onErrorResponse(E error);
    }

    /**
     * Make a GET request and return a parsed object from JSON.
     *
     * @param url         URL of the request to make
     * @param clazz       Relevant class object, for Gson's reflection
     * @param jsonRequest Request body
     * @param headers     Map of request headers
     */
    public GsonRequest(int method,
                       String url,
                       Class<T> clazz,
                       Class<E> errorClazz,
                       JSONObject jsonRequest,
                       Map<String, String> headers,
                       Listener<T> listener,
                       ErrorListener<E> errorListener) {
        super(method, url, null);

        this.clazz = clazz;
        this.errorClazz = errorClazz;

        this.listener = listener;
        this.errorListener = errorListener;

        this.headers = headers;
        this.mRequestBody = (jsonRequest == null) ? null : jsonRequest.toString();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected void deliverResponse(T response) {
        listener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        if (errorListener != null) {
            if (error.networkResponse == null || error.networkResponse.data == null) {
                errorListener.onErrorResponse(null);
                return;
            }
            try {
                String json = new String(
                        error.networkResponse.data,
                        HttpHeaderParser.parseCharset(error.networkResponse.headers));
                errorListener.onErrorResponse(gson.fromJson(json, errorClazz));
            } catch (UnsupportedEncodingException e) {
                Log.e("GsonRequest", "UnsupportedEncodingException");
                errorListener.onErrorResponse(null);
            } catch (JsonSyntaxException e) {
                Log.e("GsonRequest", "JsonSyntaxException");
                errorListener.onErrorResponse(null);
            }
        }
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(
                    response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            return Response.success(
                    gson.fromJson(json, clazz),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    @Override
    public byte[] getBody() {
        try {
            return mRequestBody == null ? null : mRequestBody.getBytes(PROTOCOL_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                    mRequestBody, PROTOCOL_CHARSET);
            return null;
        }
    }
}
