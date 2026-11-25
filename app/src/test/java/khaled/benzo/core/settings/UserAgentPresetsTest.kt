package khaled.benzo.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentPresetsTest {
    @Test fun blankIsDefault() = assertEquals(0, UserAgentPresets.indexOf("  "))
    @Test fun knownStringMatches() = assertEquals(2, UserAgentPresets.indexOf(UserAgentPresets.values[2] + " "))
    @Test fun unknownIsCustom() = assertEquals(UserAgentPresets.CUSTOM, UserAgentPresets.indexOf("MyClient/1.0"))
    @Test fun presetsArePlainAsciiAndFitTheSetting() =
        assertTrue(UserAgentPresets.values.all { it.length <= 300 && it.none { c -> c.code < 32 || c.code > 126 } })
}
