/*
 * Copyright 2012 CodeSlap
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

package com.codeslap.test.persistence;

import android.content.UriMatcher;
import android.net.Uri;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author cristian
 */
@Implements(UriMatcher.class)
public class ShadowUriMatcher {
    public static class MatchNode {
        public int code = UriMatcher.NO_MATCH;
        public HashMap<String, MatchNode> map = new HashMap<String, ShadowUriMatcher.MatchNode>();
        public MatchNode number;
        public MatchNode text;

        public MatchNode(int code) {
            this.code = code;
        }
    }

    public MatchNode rootNode;

    public void __constructor__(int code) {
        rootNode = new MatchNode(code);
    }

    @Implementation
    public void addURI(String authority, String path, int code) {
        MatchNode authNode = rootNode.map.get(authority);
        if (authNode == null) {
            authNode = new MatchNode(rootNode.code);
            rootNode.map.put(authority, authNode);
        }

        String[] segments = path.split("/");
        addNodes(authNode, Arrays.asList(segments), code);
    }

    @Implementation
    public int match(Uri uri) {
        String auth = uri.getAuthority();
        List<String> segments = uri.getPathSegments();

        if (!rootNode.map.containsKey(auth)) {
            return rootNode.code;
        }

        return matchSegments(rootNode.map.get(auth), segments);
    }

    private int matchSegments(MatchNode node, List<String> segments) {
        if (segments.isEmpty()) return node.code;
        String segment = segments.get(0);
        segments = segments.subList(1, segments.size());

        if (node.map.containsKey(segment)) {
            return matchSegments(node.map.get(segment), segments);
        }
        if (node.number != null) {
            long id;
            try {
                id = Long.parseLong(segment);
                if (id >= 0) {
                    return matchSegments(node.number, segments);
                }
            } catch (NumberFormatException e) {
            }
        }
        if (node.text != null) {
            return matchSegments(node.text, segments);
        }

        return rootNode.code;
    }

    private void addNodes(MatchNode baseNode, List<String> segments, int code) {
        MatchNode nextNode = null;
        String segment = segments.get(0);

        if (segment.equals("#")) {
            nextNode = baseNode.number;
            if (nextNode == null) {
                nextNode = new MatchNode(rootNode.code);
                baseNode.number = nextNode;
            }
        } else if (segment.equals("*")) {
            nextNode = baseNode.text;
            if (nextNode == null) {
                nextNode = new MatchNode(rootNode.code);
                baseNode.text = nextNode;
            }
        } else {
            nextNode = baseNode.map.get(segment);
            if (nextNode == null) {
                nextNode = new MatchNode(rootNode.code);
                baseNode.map.put(segment, nextNode);
            }
        }

        if (segments.size() > 1) {
            addNodes(nextNode, segments.subList(1, segments.size()), code);
        } else {
            nextNode.code = code;
        }
    }

}
