/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.liuyan.im;

import io.netty.util.internal.StringUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class HttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    static private OkHttpClient client = new OkHttpClient();

    public static void httpJsonPost(final String url, final String jsonStr){
        LOG.info("POST to {} with data {}", url, jsonStr);
        if (StringUtil.isNullOrEmpty(url)) {
            LOG.error("http post failure with empty url");
            return;
        }

        RequestBody body = RequestBody.create(JSON, jsonStr);
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                LOG.info("POST to {} with data {} failure", url, jsonStr);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                LOG.info("POST to {} success with response: {}", url, response.body());
                try {
                    if (response.body() != null) {
                        response.body().close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
