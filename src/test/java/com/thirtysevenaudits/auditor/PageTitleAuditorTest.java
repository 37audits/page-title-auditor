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

import com.thirtysevenaudits.auditor.aws.AbstractLambdaAuditorTest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PageTitleAuditorTest extends AbstractLambdaAuditorTest {

    @Test
    void missingTitle_shouldFail() throws Exception {
        startServer(Map.of("/", html("<html><head></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.FAIL);
        assertThat(r.message()).isEqualTo("Title tag not found");
    }

    @Test
    void emptyTitle_shouldFail() throws Exception {
        startServer(Map.of("/", html("<html><head><title></title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.FAIL);
        assertThat(r.message()).isEqualTo("Title tag is empty");
    }

    @Test
    void whitespaceOnlyTitle_shouldFail() throws Exception {
        startServer(Map.of("/", html("<html><head><title>   </title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.FAIL);
        assertThat(r.message()).isEqualTo("Title tag is empty");
    }

    @Test
    void optimalLengthTitle_shouldSuccess() throws Exception {
        String title = "This is a Perfect Title"; // 23 characters, within 15-70 range
        startServer(Map.of("/", html("<html><head><title>" + title + "</title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(r.message()).isEqualTo("Page title length is within the recommended range (15–70 characters)");
        assertThat(r.checks().get(0).data().get("title")).isEqualTo(title);
        assertThat(r.checks().get(0).data().get("length")).isEqualTo(23);
    }

    @Test
    void minimumLengthTitle_shouldSuccess() throws Exception {
        String title = "Minimum 15 Char"; // Exactly 15 characters
        startServer(Map.of("/", html("<html><head><title>" + title + "</title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(r.message()).isEqualTo("Page title length is within the recommended range (15–70 characters)");
    }

    @Test
    void maximumLengthTitle_shouldSuccess() throws Exception {
        String title = "This is exactly a seventy character title for testing maximum len!"; // Exactly 70 characters
        startServer(Map.of("/", html("<html><head><title>" + title + "</title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(r.message()).isEqualTo("Page title length is within the recommended range (15–70 characters)");
    }

    @Test
    void tooShortTitle_shouldWarn() throws Exception {
        String title = "Short"; // 5 characters, less than 15
        startServer(Map.of("/", html("<html><head><title>" + title + "</title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.WARNING);
        assertThat(r.message()).isEqualTo("Page title is too short(less than 15 characters)");
    }

    @Test
    void slightlyTooLongTitle_shouldWarn() throws Exception {
        String title = "This is a title that is slightly longer than the recommended seventy char"; // 80 characters
        startServer(Map.of("/", html("<html><head><title>" + title + "</title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.WARNING);
        assertThat(r.message()).isEqualTo("Page title is too long (more than 70 characters)");
    }

    @Test
    void excessivelyLongTitle_shouldFail() throws Exception {
        String title = "This is an excessively long title that goes way beyond what is reasonable for a page title and will definitely be truncated in search engine results pages making it completely useless for SEO purposes"; // 200+
                                                                                                                                                                                                                                   // characters
        startServer(Map.of("/", html("<html><head><title>" + title + "</title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.FAIL);
        assertThat(r.message()).isEqualTo(
                "Page title is excessively long (more than 100 characters). This may be truncated in search results");
    }

    @Test
    void titleWithSpecialCharacters_shouldWork() throws Exception {
        String title = "Special Characters: @#$%^&*()"; // 30 characters with special chars
        startServer(Map.of("/", html("<html><head><title>" + title + "</title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(r.checks().get(0).data().get("title")).isEqualTo(title);
    }

    @Test
    void titleWithLeadingTrailingWhitespace_shouldTrim() throws Exception {
        String title = "   Trimmed Title   ";
        String expectedTitle = "Trimmed Title"; // 13 characters after trimming
        startServer(Map.of("/", html("<html><head><title>" + title + "</title></head><body>ok</body></html>")));
        PageTitleAuditor checker = new PageTitleAuditor();
        Response r = checker.process(baseUrl(), null);
        assertThat(r).isNotNull();
        assertThat(r.status()).isEqualTo(CheckStatus.WARNING); // Less than 15 characters
        assertThat(r.checks().get(0).data().get("title")).isEqualTo(expectedTitle);
        assertThat(r.checks().get(0).data().get("length")).isEqualTo(13);
    }
}
