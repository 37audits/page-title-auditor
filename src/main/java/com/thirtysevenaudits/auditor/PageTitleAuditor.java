/*
 * Copyright © 2026 37 Audits (thiago.moreira@37audits.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thirtysevenaudits.auditor;

import com.thirtysevenaudits.auditor.aws.AbstractLambdaAuditor;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.Connection;

/**
 * Checks if the HTML page title follows best practices. The title must be present and be between 15 and 70 characters
 * for optimal SEO and usability. This implementation fetches the HTML, uses jsoup to parse it and extract the title
 * tag, then evaluates its length. Titles between 15-70 characters are considered optimal, below 15 or above 70 trigger
 * warnings or failures.
 */
public class PageTitleAuditor extends AbstractLambdaAuditor {

    @Override
    public String getName() {
        return "Page Title";
    }

    @Override
    public Response process(String urlStr, BasicAuth basicAuth) {
        try {
            Connection connection = Jsoup.connect(urlStr).userAgent(getUserAgent());

            // Add basic auth if provided
            if (basicAuth != null) {
                String userAndPassword = basicAuth.username() + ":" + basicAuth.password();
                String authValue = Base64.getEncoder().encodeToString(userAndPassword.getBytes());
                connection.header("Authorization", "Basic " + authValue);
            }

            Document doc = connection.get();
            Element titleElement = doc.selectFirst("title");

            if (titleElement == null) {
                return new Response(getAuditor(), CheckStatus.FAIL, "Title tag not found", null);
            }

            String title = titleElement.text().trim();
            int length = title.length();

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("length", length);

            List<Check> checks = new LinkedList<Check>();
            if (length == 0) {
                checks.add(new Check(CheckStatus.FAIL, urlStr, "Title tag is empty", null, 0, data));
            } else if (length >= 15 && length <= 70) {
                checks.add(new Check(CheckStatus.SUCCESS, urlStr,
                        "Page title length is within the recommended range (15–70 characters)", null, 0, data));
            } else if (length < 15) {
                checks.add(new Check(CheckStatus.WARNING, urlStr, "Page title is too short(less than 15 characters)",
                        "Consider adding more descriptive content up to 70 characters", 0, data));
            } else if (length <= 100) {
                checks.add(new Check(CheckStatus.WARNING, urlStr, "Page title is too long (more than 70 characters)",
                        "Consider up to 70 characters shortening for better display in search results", 0, data));
            } else {
                checks.add(new Check(CheckStatus.FAIL, urlStr,
                        "Page title is excessively long (more than 100 characters). This may be truncated in search results",
                        "Consider shortening up to 70 characters for better display in search results", 0, data));
            }

            CheckStatus status = checks.get(0).status();
            String message = checks.get(0).message();
            return new Response(getAuditor(), status, message, checks);

        } catch (Exception e) {
            return new Response(getAuditor(), CheckStatus.FAIL, e.getMessage(), null);
        }
    }
}
