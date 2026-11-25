package khaled.benzo.ui.common

import khaled.benzo.R
import khaled.benzo.core.storage.FolderCheck.Verdict

/** Maps a folder check result to the text the user sees. */
object FolderMessages {
    fun res(v: Verdict): Int = when (v) {
        Verdict.NOT_A_FOLDER -> R.string.folder_problem_not_a_folder
        Verdict.EMPTY -> R.string.folder_problem_empty
        Verdict.NOT_ABSOLUTE -> R.string.folder_problem_not_absolute
        Verdict.NOT_WRITABLE, Verdict.OK -> R.string.folder_problem_not_writable
    }
}
