package org.jetbrains.kotlin.spec.loader

import js.externals.jquery.JQueryAjaxSettings
import js.externals.jquery.JQueryPromise
import js.externals.jquery.JQueryXHR
import js.externals.jquery.`$`
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.kotlin.spec.entity.Section
import org.jetbrains.kotlin.spec.entity.SectionTestMap
import org.jetbrains.kotlin.spec.entity.test.Test
import org.jetbrains.kotlin.spec.entity.test.TestPlace
import org.jetbrains.kotlin.spec.entity.test.parameters.TestInfo
import org.jetbrains.kotlin.spec.entity.test.parameters.testArea.TestArea
import org.jetbrains.kotlin.spec.utils.format
import kotlin.browser.window
import kotlin.js.Promise

interface GithubTestsLoader {
    enum class TestFileType { SPEC_TEST, IMPLEMENTATION_TEST }

    companion object {
        private const val RAW_GITHUB_URL = "https://raw.githubusercontent.com/JetBrains/kotlin"

        private const val SPEC_TEST_DATA_PATH = "compiler/tests-spec/testData"
        private const val LINKED_SPEC_TESTS_FOLDER = "linked"

        const val DEFAULT_BRANCH = "spec-tests"

        fun getBranch() = window.localStorage.getItem("spec-tests-branch") ?: DEFAULT_BRANCH

        fun loadTestFileFromRawGithub(
                path: String,
                testPathInSpec: String? = null,
                customFolder: String? = null,
                testInfo: TestInfo,
                testPlace: TestPlace,
                testArea: TestArea
        ): Promise<Test> {
            return Promise { requestResolve, requestReject ->
                val testContainerMap = mutableMapOf<String, String>()

                val testsPaths = TestsPaths(customFolder, path, testPathInSpec) //todo get rid of ?

                val queryForTestArea: JQueryPromise<Unit> = getQueryForTest(testContainerMap, requestReject, testsPaths)

                `$`.`when`(queryForTestArea).then({ _: Any?, _: Any ->
                    requestResolve(Test(testInfo, testPlace, testContainerMap["content"]!!, testArea))
                })
            }
        }

        fun loadTestMapFileFromRawGithub(
                path: String,
                testPathInSpec: String? = null,
                testType: TestFileType,
                customFolder: String? = null,
                testAreasToLoad: Array<TestArea>
        ): Promise<MutableMap<TestArea, SectionTestMap>> {
            return Promise { resolve, _ ->
                val resultMap = mutableMapOf<TestArea, SectionTestMap>()
                val testsPaths = TestsPaths(customFolder, path, testPathInSpec)
                val querySet = mutableSetOf<JQueryPromise<Unit>>()
                testAreasToLoad.forEach { testArea ->
                    querySet.add(
                            getQueryForTestMap(testArea, resultMap, testType, testsPaths)
                    )
                }
                `$`.`when`(*(querySet.toTypedArray()))
                        .then({ _: Any?, _: Any ->
                            resolve(resultMap)
                        }, {
                            resolve(resultMap)
                        })
            }
        }


        class TestsPaths(
                val customFolder: String?,
                val path: String, val testPathInSpec: String?
        )

        private fun getFullPath(testFileType: TestFileType, testArea: TestArea, customFolder: String?, path: String) =
                when (testFileType) {
                    TestFileType.SPEC_TEST -> "{1}/{2}/{3}/{4}/{5}/{6}"
                            .format(RAW_GITHUB_URL, getBranch(), SPEC_TEST_DATA_PATH, testArea.path, customFolder
                                    ?: LINKED_SPEC_TESTS_FOLDER, path)
                    TestFileType.IMPLEMENTATION_TEST -> "{1}/{2}/{3}".format(RAW_GITHUB_URL, getBranch(), path)
                }

        private fun getFullPathForTest(path: String) = "{1}/{2}/{3}".format(RAW_GITHUB_URL, getBranch(), path)

        private fun getQueryForTestMap(
                testArea: TestArea,
                resultMapSection: MutableMap<TestArea, SectionTestMap>,
                testType: TestFileType,
                testsPaths: TestsPaths
        ): JQueryPromise<Unit> = `$`.ajax(
                getFullPath(testType, testArea, testsPaths.customFolder, testsPaths.path),
                jQueryAjaxSettings { }).then(
                doneFilter = { response: Any?, _: Any ->
                    val testMapData = SectionTestMap(sectionTestMap = parseTestsMapFile(response.toString()), testArea = testArea)
                    resultMapSection[testArea] = testMapData
                }
        )


        private fun parseTestsMapFile(testsMapText: String) = Json(JsonConfiguration.Stable).parseJson(testsMapText)

        private fun getQueryForTest(
                testContainerMap: MutableMap<String, String>,
                requestReject: (Throwable) -> Unit,
                testsPaths: TestsPaths
        ) = `$`.ajax(getFullPathForTest(testsPaths.path),
                jQueryAjaxSettings { requestReject(Exception()) }).then(
                { response: Any?, _: Any ->
                    testContainerMap["content"] = response.toString()
                    testContainerMap["contentPath"] = (testsPaths.testPathInSpec ?: testsPaths.path)
                },
                { }
        )

        private fun jQueryAjaxSettings(requestReject: (Throwable) -> Unit): JQueryAjaxSettings {
            return object : JQueryAjaxSettings { //TODO here?
                override var cache: Boolean?
                    get() = false
                    set(_) {}
                override var type: String?
                    get() = "GET"
                    set(_) {}
                override val error: ((jqXHR: JQueryXHR, textStatus: String, errorThrown: String) -> Any)?
                    get() = { _, _, _ -> requestReject(Exception()) }
            }
        }
    }

    fun loadTestFiles(sectionName: String, sectionsPath: List<String>, testAreasToLoad: Array<TestArea>): Promise<Promise<Section>>
}