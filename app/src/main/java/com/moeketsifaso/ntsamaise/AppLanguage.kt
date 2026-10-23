package com.moeketsifaso.ntsamaise

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String) {
    ENGLISH("en"), SESOTHO("st");

    companion object {
        fun fromCode(code: String?): AppLanguage? = entries.firstOrNull { it.code == code }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

/** English app results bypass translation entirely; Sesotho retains the offline/cloud pipeline. */
internal suspend fun AppLanguage.output(text: String): TranslationResult =
    if (this == AppLanguage.ENGLISH) TranslationResult(text, text, TranslationSource.ORIGINAL)
    else GoogleCloudTranslator.translate(text)

internal fun AppLanguage.ui(text: String): String =
    if (this == AppLanguage.ENGLISH) text else sesothoUi[text] ?: text

/** App-owned interface copy is available without a network or API key. */
internal val sesothoUi = mapOf(
    "Your visual assistant" to "Mothusi oa hau ea hloa-hloa",
    "Change language" to "Fetola puo",
    "Object confidence" to "Kholiseho ea temoho ea ntho",
    "Colour confidence" to "Kholiseho ea 'mala",
    "Low" to "E tlase",
    "Moderate" to "E mahareng",
    "Scan Surroundings" to "Hlahloba tikoloho",
    "Detect Obstacles" to "Fumana litšitiso",
    "Identify Money" to "Tseba chelete",
    "Identify Product" to "Tseba sehlahisoa",
    "Read Text" to "Bala mongolo",
    "Back to home" to "Khutlela hae",
    "Scan banknote" to "Hlahloba chelete",
    "Capture and read" to "Tšoara 'me u bale",
    "Repeat result" to "Pheta sephetho",
    "Read again" to "Bala hape",
    "Cancel capture" to "Hlakola ho tšoara",
    "Stop reading" to "Emisa ho bala",
    "Recognizing text…" to "E bala mongolo…",
    "Recognizing text. Hold the camera steady." to "E bala mongolo. Tšoara khamera e sa tsitsinyehe.",
    "Text captured. Tap Read again to read it aloud." to "Mongolo o tšoeroe. Tobetsa Bala hape ho o utloa.",
    "Text captured and translated. Tap Read again to repeat." to "Mongolo o tšoeroe le ho fetoleloa. Tobetsa Bala hape ho pheta.",
    "Point the camera at printed text, then tap Capture and read." to "Lebisa khamera mongolong o hatisitsoeng, ebe u tobetsa Tšoara 'me u bale.",
    "Place one Lesotho note flat. Show the whole side with the bank name, then tap Scan banknote." to "Beha chelete e le 'ngoe ea Lesotho e bataletse. Bontša lehlakore lohle le nang le lebitso la banka, ebe u tobetsa Hlahloba chelete.",
    "Text-based prototype for 10, 20, 50, 100 and 200 maloti notes. Coins and other currencies are not supported. It cannot check whether a note is genuine." to "Mofuta oa teko o bala mongolo oa lipampiri tsa maloti a 10, 20, 50, 100 le 200. Ha o tšehetse lichelete tsa tšepe kapa tsa linaha tse ling. Ha o tiise hore chelete ke ea 'nete.",
    "Capture cancelled. Try again when ready." to "Ho tšoara ho hlakotsoe. Leka hape ha u se u loketse.",
    "Translating currency result to Sesotho..." to "E fetolela sephetho sa chelete ho Sesotho...",
    "Translating to Sesotho..." to "E fetolela ho Sesotho...",
    "Result" to "Sephetho",
    "Recognized text" to "Mongolo o fumanoeng",
    "Original text" to "Mongolo oa pele",
    "Sesotho translation unavailable. Showing original text." to "Phetolelo ea Sesotho ha e fumanehe. Ho bontšoa mongolo oa pele.",
    "Results are suggestions from printed text." to "Liphetho ke likhakanyo tse tsoang mongolong o hatisitsoeng.",
    "Printed text is read in your selected language when translation is available." to "Mongolo o baloa ka puo eo u e khethileng ha phetolelo e fumaneha.",
    "Allow camera" to "Lumella khamera",
    "Camera permission is required. If denied permanently, enable it in the app's settings." to "Tumello ea khamera ea hlokahala. Ha e hanetsoe, e lumelle litlhophisong tsa sesebelisoa.",
    "Recognition unavailable. Reopen this screen to retry." to "Temoho ha e fumanehe. Bula leqephe lena hape ho leka.",
    "Could not recognize this image. Try again with the camera steady." to "Setšoantšo ha sea lemohuoa. Leka hape u tšoere khamera e sa tsitsinyehe.",
    "Camera unavailable. Reopen this screen to retry." to "Khamera ha e fumanehe. Bula leqephe lena hape ho leka.",
    "Could not read this image. Hold the camera steady and try again." to "Setšoantšo ha sea baleha. Tšoara khamera e sa tsitsinyehe 'me u leke hape.",
    "Could not read aloud. Check your phone's text-to-speech settings." to "Ha e khone ho bala ka lentsoe. Hlahloba litlhophiso tsa puo fonong ea hau.",
    "Speech unavailable. Install an English or Sesotho voice in your phone's text-to-speech settings." to "Lentsoe ha le fumanehe. Kenya lentsoe la Senyesemane kapa Sesotho litlhophisong tsa puo fonong ea hau.",
    "Speech unavailable. Check your phone's text-to-speech settings." to "Lentsoe ha le fumanehe. Hlahloba litlhophiso tsa puo fonong ea hau.",
    "Waiting for camera..." to "E emetse khamera...",
    "Looking for objects..." to "E batla lintho...",
    "No text found. Hold one note flat in good light and scan again." to "Ha ho mongolo o fumanoeng. Beha chelete e le 'ngoe hantle leseling 'me u hlahlobe hape.",
    "No text found. Improve the lighting, move closer and try again." to "Ha ho mongolo o fumanoeng. Ntlafatsa leseli, atametsa 'me u leke hape.",
    "Resume detection" to "Tsoela pele ho fumana litšitiso",
    "Pause detection" to "Emisa ho fumana litšitiso",
    "Detection paused" to "Ho fumana litšitiso ho emisitsoe",
    "No recent camera results. Pause and resume detection to retry." to "Ha ho liphetho tsa morao-rao tsa khamera. Emisa 'me u qale hape.",
    "Looking for possible obstacles..." to "E batla litšitiso tse ka bang teng...",
    "No supported obstacle detected. This does not mean the path is clear." to "Ha ho tšitiso e lemohuoeng. Sena ha se bolele hore tsela e bolokehile.",
    "Distance unavailable — calibrate and hold the same whole object steady." to "Sebaka ha se fumanehe — lekanya 'me u bontše ntho eona eo kaofela e sa sisinyehe.",
    "Distance calibration" to "Ho lekanya sebaka",
    "Measured reference distance (metres)" to "Sebaka se lekantsoeng (limithara)",
    "Enter a measured distance from 0.3 to 10 metres." to "Kenya sebaka se lekantsoeng ho tloha ho 0.3 ho isa ho 10 limithara.",
    "Waiting for a steady, fully visible object with sufficient confidence." to "E emetse ntho e sa sisinyeheng e bonahalang kaofela le e lemohuoang hantle.",
    "Calibrate distance" to "Lekanya sebaka",
    "Clear calibration" to "Hlakola tekanyo",
    "Estimated distance" to "Khakanyo ea sebaka",
    "Calibrated for" to "E lekanyelitsoe bakeng sa",
    "Ready to calibrate" to "E loketse ho lekanya",
    "Experimental camera alerts. Hold the phone upright, facing forward. Left and right refer to the camera view. Calibrated distances are approximate. Steps, potholes and other hazards may be missed. Keep using your usual mobility aid." to "Litemoso tsa khamera ke tsa teko. Tšoara fono e eme e shebile pele. Letšehali le letona li latela khamera. Libaka tse lekantsoeng ke likhakanyo. Litepisi, likoti le likotsi tse ling li ka se bonahale. Tsoela pele ho sebelisa thuso ea hau ea ho tsamaea.",
    "With a helper, measure from the camera to one stationary object (0.3–10 metres). Show the whole object, hold steady, enter that distance and tap Calibrate. Estimates apply only to this same object at the same angle. Do not switch objects or zoom. Calibration clears when detection stops, the camera rotates, or the selected object class changes." to "Ka mothusi, lekanya ho tloha khamereng ho ea nthong e le 'ngoe e sa sisinyeheng (0.3–10 limithara). Bontša ntho kaofela, tšoara khamera hantle, kenya sebaka ebe u tobetsa Lekanya sebaka. Likhakanyo li sebetsa feela nthong eona eo ka lehlakore lona leo. U se ke ua fetola ntho kapa ua atametsa khamera. Tekanyo ea hlakoloa ha temoho e emisa, khamera e potoloha kapa mofuta oa ntho o fetoha."
)
