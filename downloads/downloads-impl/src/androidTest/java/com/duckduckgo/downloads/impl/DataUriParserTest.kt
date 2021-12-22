/*
 * Copyright (c) 2022 DuckDuckGo
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

package com.duckduckgo.downloads.impl

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DataUriParserTest {

    private lateinit var testee: DataUriParser

    @Before
    fun setup() {
        testee = DataUriParser(DataUriSuffixParser())
    }

    @Test
    fun whenMimeTypeProvidedAsImagePngThenPngSuffixGenerated() {
        val parsed = testee.generate("data:image/png;base64,AAAA") as DataUriParser.ParseResult.ParsedDataUri
        Assert.assertEquals("png", parsed.filename.fileType)
    }

    @Test
    fun whenMimeTypeProvidedAsImageJpegThenJpgSuffixGenerated() {
        val parsed = testee.generate("data:image/jpeg;base64,AAAA") as DataUriParser.ParseResult.ParsedDataUri
        Assert.assertEquals("jpg", parsed.filename.fileType)
    }

    @Test
    fun whenMimeTypeProvidedAsArbitraryImageTypeThenNoSuffixGenerated() {
        val parsed = testee.generate("data:image/foo;base64,AAAA") as DataUriParser.ParseResult.ParsedDataUri
        Assert.assertEquals("", parsed.filename.fileType)
    }

    @Test
    fun whenMimeTypeNotProvidedThenNoSuffixAdded() {
        val parsed = testee.generate("data:,AAAA") as DataUriParser.ParseResult.ParsedDataUri
        Assert.assertEquals("", parsed.filename.fileType)
    }

    @Test
    fun whenInvalidDataUriProvidedInvalidTypeTurned() {
        val parsed = testee.generate("AAAA")
        Assert.assertTrue(parsed === DataUriParser.ParseResult.Invalid)
    }

    @Test
    fun whenKnownMimeTypeProvidedAsNonImageTypeThenSuffixStillGenerated() {
        val parsed = testee.generate("data:text/plain;base64,AAAA") as DataUriParser.ParseResult.ParsedDataUri
        Assert.assertEquals("txt", parsed.filename.fileType)
    }

    @Test
    fun whenMimeTypeNotProvidedThenNoSuffixAddedInToString() {
        val filename = testee.generate("data:,AAAA") as DataUriParser.ParseResult.ParsedDataUri
        Assert.assertFalse(filename.toString().contains("."))
    }
}
