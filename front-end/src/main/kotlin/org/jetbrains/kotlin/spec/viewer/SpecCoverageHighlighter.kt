package org.jetbrains.kotlin.spec.viewer

import js.externals.jquery.JQuery
import js.externals.jquery.`$`
import org.jetbrains.kotlin.spec.entity.Paragraph
import org.jetbrains.kotlin.spec.entity.Section
import org.jetbrains.kotlin.spec.entity.Sentence
import org.jetbrains.kotlin.spec.entity.test.Test
import org.jetbrains.kotlin.spec.utils.format
import org.w3c.dom.HTMLElement

object SpecCoverageHighlighter {

    const val TEMPLATE = """
            <div class='test-coverage-view'>
                <select name='test-area'>
                    <option value='diagnostics'>Front-end diagnostics tests</option>
                    <option value='box'>Codegen box tests</option>
                </select>
                <select name='test-type'></select>
                <select name='test-link-type'>
                    <option value='main'>Main tests</option>
                    <option value='primary'>Primary tests</option>
                    <option value='secondary'>Secondary tests</option>
                </select>
                <select name='test-number'></select>
            </div>
        """


    private fun List<Test>.detectUnexpectedBehaviour(): Boolean {
        this.forEach {
            if (it.testInfo.unexpectedBehaviour)
                return true
        }
        return false
    }

    private fun showSentenceCoverage(sentence: JQuery, sentenceTestSet: Sentence) {
        val testsByArea = mutableListOf<String>()
        var unexpectedBehaviour = false

        for ((testArea, listOfTests) in sentenceTestSet.loadedTestAreas) {

            val map = listOfTests.groupBy { it.testPlace.testType }

            val testNumberByTypeInfo = mutableListOf<String>()
            for ((posOrNeg, typedTests) in map) {
                unexpectedBehaviour = unexpectedBehaviour || typedTests.detectUnexpectedBehaviour()

                testNumberByTypeInfo.add(typedTests.size.toString() + " " + posOrNeg.toString())

                if (testNumberByTypeInfo.size > 0) {
                    testsByArea.add("<b>" + testArea.description + "</b>: " + testNumberByTypeInfo.joinToString(", "))
                }
            }

        }

        if (unexpectedBehaviour) {
            sentence.addClass("unexpected-behaviour")
                    .parent().before("<span class='unexpected-behaviour-marker'></span>")
        }

        if (testsByArea.isNotEmpty()) {
            sentence.prepend("<span class='coverage-info'>{1}</span>".format(testsByArea.joinToString("<br />")))
                    .data("tests", sentenceTestSet)
                    .addClass("covered")

        }
    }

    private fun showParagraphCoverage(paragraph: JQuery, sectionPath: String, paragraphTestSet: Paragraph) {
        val sentences = `$`(paragraph).find(".sentence")
        var sentenceCounter = 1
        val paragraphNumber = paragraphTestSet.paragraph

        MarkUpArranger.insertParagraphNumber(`$`(paragraph), paragraphNumber, sectionPath, paragraphNumber)

        sentences.each { _, el ->
            val sentence = `$`(el)

            val existingNumberInfo = sentence.find(".number-info")
            if (existingNumberInfo.length.toInt() > 0) {
                existingNumberInfo.remove()
            }

            val sentenceTestSet = Sentence(paragraph = paragraphTestSet,
                    sentence = sentenceCounter)
            showSentenceCoverage(sentence, sentenceTestSet)
            MarkUpArranger.insertSentenceNumber(sentence, sentenceCounter, sectionPath, paragraphNumber, sentenceCounter)
            sentenceCounter++
        }
    }


    fun showCoverageOfParagraphs(paragraphsInfo: List<Map<String, Any>>, sectionTestSet: Section, sectionsPath: String) {
        paragraphsInfo.forEachIndexed { paragraphIndex, paragraph ->
            val paragraphNumber = paragraphIndex + 1
            val paragraphEl = `$`(paragraph["paragraphElement"] as HTMLElement).apply { addClass("with-tests") }

            val paragraphTestSet = Paragraph(section = sectionTestSet,
                    paragraph = paragraphNumber)
            showParagraphCoverage(paragraphEl, sectionsPath, paragraphTestSet)
        }
    }
}
