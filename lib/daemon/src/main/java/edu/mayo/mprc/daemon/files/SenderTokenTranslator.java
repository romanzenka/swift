package edu.mayo.mprc.daemon.files;

import java.io.File;

/**
 * Translates a {@link FileToken} to a version that is suitable for transfer over the wire.
 */
public interface SenderTokenTranslator {
	/**
	 * Translate a {@link FileToken} before it gets transferred. As a {@link FileTokenHolder} you
	 * have to translate all your tokens using this method and replace your original tokens with the translated versions.
	 *
	 * @param fileToken Token to be translated.
	 * @return Translated token. Store it in place of your original one (you are not losing any information here).
	 */
	FileToken translateBeforeTransfer(FileToken fileToken);

	/**
	 * In case your {@link FileTokenHolder} has a file to start with, this method will obtain the {@link FileToken} directly.
	 * The source daemon is set to the currently running one.
	 *
	 * @param file File to translate to the token.
	 * @return FileToken corresponding to a local file.
	 */
	FileToken translateBeforeTransfer(File file);
}
