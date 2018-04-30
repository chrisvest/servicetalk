/*
 * Copyright © 2018 Apple Inc. and the ServiceTalk project authors
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
 */
package io.servicetalk.http.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Collections.addAll;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public abstract class AbstractHttpRequestMetaDataTest<T extends HttpRequestMetaData> {

    @Rule
    public final ExpectedException expected = ExpectedException.none();

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    protected T fixture;

    final Map<String, List<String>> params = new LinkedHashMap<>();

    protected abstract void createFixture(String uri);

    protected abstract void setFixtureQueryParams(Map<String, List<String>> params);

    // https://tools.ietf.org/html/rfc7230#section-5.3.1
    @Test
    public void testParseUriOriginForm() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");

        assertEquals("/some/path", fixture.getPath());
        assertEquals("/some/path", fixture.getRawPath());
        assertEquals("foo=bar&abc=def&foo=baz", fixture.getRawQuery());
        assertEquals("/some/path?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
    }

    // https://tools.ietf.org/html/rfc7230#section-5.3.2
    @Test
    public void testParseHttpUriAbsoluteForm() {
        // TODO: need to include user-info
        createFixture("http://my.site.com/some/path?foo=bar&abc=def&foo=baz");

        assertEquals("/some/path", fixture.getPath());
        assertEquals("/some/path", fixture.getRawPath());
        assertEquals("foo=bar&abc=def&foo=baz", fixture.getRawQuery());
        assertEquals("http://my.site.com/some/path?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
    }

    @Test
    public void testParseHttpsUriAbsoluteForm() {
        // TODO: need to include user-info
        createFixture("https://my.site.com/some/path?foo=bar&abc=def&foo=baz");

        assertEquals("/some/path", fixture.getPath());
        assertEquals("/some/path", fixture.getRawPath());
        assertEquals("foo=bar&abc=def&foo=baz", fixture.getRawQuery());
        assertEquals("https://my.site.com/some/path?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
    }

    @Test
    public void testSetPathWithoutLeadingSlash() {
        createFixture("temp");
        fixture.setPath("foo");
        assertEquals("/foo", fixture.getRequestTarget());
    }

    @Test
    public void testSetRawPathWithoutLeadingSlash() {
        createFixture("temp");
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("Path must be empty or start with '/'");
        fixture.setRawPath("foo");
    }

    @Test
    public void testReplacePathInAbsoluteForm() {
        createFixture("http://my.site.com/some/path?foo=bar&abc=def&foo=baz");
        fixture.setPath("/new/$path$");

        assertEquals("http://my.site.com/new/%24path%24?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
    }

    @Test
    public void testSetEmptyPathInAbsoluteForm() {
        createFixture("http://my.site.com/some/path?foo=bar&abc=def&foo=baz");
        fixture.setPath("");

        assertEquals("http://my.site.com?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
    }

    @Test
    public void testReplacePathInOriginForm() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");
        fixture.setPath("/new/$path$");

        assertEquals("/new/%24path%24?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
    }

    @Test
    public void testSetEmptyPathInOriginForm() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");
        fixture.setPath("");

        assertEquals("?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
    }

    @Test
    public void testReplaceExistingQuery() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");
        fixture.setRawQuery("new=query");

        assertEquals("/some/path?new=query", fixture.getRequestTarget());
    }

    @Test
    public void testSetRemoveExistingQuery() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");
        fixture.setRawQuery("");

        assertEquals("/some/path", fixture.getRequestTarget());
    }

    @Test
    public void testAddQuery() {
        createFixture("/some/path");
        fixture.setRawQuery("new=query");

        assertEquals("/some/path?new=query", fixture.getRequestTarget());
    }

    @Test
    public void testParseQuery() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");
        final HttpQuery query = fixture.parseQuery();

        assertEquals(asList("foo", "abc"), iteratorAsList(query.getKeys().iterator()));
        assertEquals("bar", query.get("foo"));
        assertEquals(asList("bar", "baz"), iteratorAsList(query.getAll("foo")));
        assertEquals("def", query.get("abc"));
        assertEquals(singletonList("def"), iteratorAsList(query.getAll("abc")));
    }

    @Test
    public void testReencodeQuery() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");
        final HttpQuery query = fixture.parseQuery();

        query.encodeToRequestTarget();
        assertEquals("/some/path?foo=bar&foo=baz&abc=def", fixture.getRequestTarget());
    }

    @Test
    public void testUriDoesNotChangeUntilReencode() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");
        final HttpQuery query = fixture.parseQuery();
        query.set("abc", "new");

        assertEquals("/some/path?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());

        query.encodeToRequestTarget();

        assertEquals("/some/path?foo=bar&foo=baz&abc=new", fixture.getRequestTarget());
    }

    @Test
    public void testSetPathAndReparse() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");

        // parse it
        assertEquals("/some/path?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
        assertEquals("/some/path", fixture.getRawPath());
        assertEquals("/some/path", fixture.getPath());
        assertEquals("foo=bar&abc=def&foo=baz", fixture.getRawQuery());
        final HttpQuery query = fixture.parseQuery();
        assertEquals(asList("bar", "baz"), iteratorAsList(query.getAll("foo")));
        assertEquals(singletonList("def"), iteratorAsList(query.getAll("abc")));

        // change it
        fixture.setPath("/new/$path$");

        // parse it again
        assertEquals("/new/%24path%24?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
        assertEquals("/new/%24path%24", fixture.getRawPath());
        assertEquals("/new/$path$", fixture.getPath());
        assertEquals("foo=bar&abc=def&foo=baz", fixture.getRawQuery());
        final HttpQuery newQuery = fixture.parseQuery();
        assertEquals(asList("bar", "baz"), iteratorAsList(newQuery.getAll("foo")));
        assertEquals(singletonList("def"), iteratorAsList(newQuery.getAll("abc")));
    }

    @Test
    public void testSetQueryAndReparse() {
        createFixture("/some/path?foo=bar&abc=def&foo=baz");

        // parse it
        assertEquals("/some/path?foo=bar&abc=def&foo=baz", fixture.getRequestTarget());
        assertEquals("/some/path", fixture.getRawPath());
        assertEquals("/some/path", fixture.getPath());
        assertEquals("foo=bar&abc=def&foo=baz", fixture.getRawQuery());
        final HttpQuery query = fixture.parseQuery();
        assertEquals(asList("bar", "baz"), iteratorAsList(query.getAll("foo")));
        assertEquals(singletonList("def"), iteratorAsList(query.getAll("abc")));

        // change it
        fixture.setRawQuery("abc=new");

        // parse it again
        assertEquals("/some/path?abc=new", fixture.getRequestTarget());
        assertEquals("/some/path", fixture.getRawPath());
        assertEquals("/some/path", fixture.getPath());
        assertEquals("abc=new", fixture.getRawQuery());
        final HttpQuery newQuery = fixture.parseQuery();
        assertEquals(singleton("abc"), newQuery.getKeys());
        assertEquals(singletonList("new"), iteratorAsList(newQuery.getAll("abc")));
    }

    @Test
    public void testEncodeToRequestTargetWithNoParams() {
        createFixture("/some/path");
        setFixtureQueryParams(params);

        assertEquals("/some/path", fixture.getRequestTarget());
    }

    @Test
    public void testEncodeToRequestTargetWithParam() {
        createFixture("/some/path");
        params.put("foo", newList("bar", "baz"));
        setFixtureQueryParams(params);

        assertEquals("/some/path?foo=bar&foo=baz", fixture.getRequestTarget());
    }

    @Test
    public void testEncodeToRequestTargetWithMultipleParams() {
        createFixture("/some/path");
        params.put("foo", newList("bar", "baz"));
        params.put("abc", newList("123", "456"));
        setFixtureQueryParams(params);

        assertEquals("/some/path?foo=bar&foo=baz&abc=123&abc=456", fixture.getRequestTarget());
    }

    @Test
    public void testEncodeToRequestTargetWithSpecialCharacters() {
        createFixture("/some/path");
        params.put("pair", newList("key1=value1", "key2=value2"));
        setFixtureQueryParams(params);

        assertEquals("/some/path?pair=key1%3Dvalue1&pair=key2%3Dvalue2", fixture.getRequestTarget());
    }

    @Test
    public void testEncodeToRequestTargetWithAbsoluteForm() {
        createFixture("http://my.site.com/some/path?foo=bar&abc=def&foo=baz");
        params.put("foo", newList("new"));
        setFixtureQueryParams(params);

        assertEquals("http://my.site.com/some/path?foo=new", fixture.getRequestTarget());
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> newList(final T... elements) {
        final List<T> list = new ArrayList<>(elements.length);
        addAll(list, elements);
        return list;
    }

    private static <T> List<T> iteratorAsList(final Iterator<T> iterator) {
        return StreamSupport
                .stream(spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .collect(toList());
    }
}
