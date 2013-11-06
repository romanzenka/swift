package edu.mayo.mprc.daemon.files;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Serializable;
import java.util.Set;

/**
 * A file token holder is simply a class that holds - directly or indirectly, objects of {@link FileToken} type.
 * <p/>
 * These classes have to perform three operations:
 * <ol>
 * <li>before the token gets sent over the wire, it needs to be translated. This makes the token wire-able.</li>
 * <li>After the token arrives to the destination, it has to be translated again using a provided {@link ReceiverTokenTranslator}.</li>
 * <li>Sometimes the file token specifies a file that is to be created by the receiver. Once the file is created, it can be synchronized
 * with the requester by calling {@link #synchronizeFileTokensOnReceiver()}</li>
 * </ul>
 */
public interface FileTokenHolder extends Serializable {

	/**
	 * Before the object gets sent over wire, this method has to translate all {@link FileToken} objects that are being
	 * held by it using the provided translator. This is done in place - the translated {@link FileToken} replaces the
	 * original.
	 */
	void translateOnSender(SenderTokenTranslator translator);

	/**
	 * After the holder is received over the wire, this method lets it translate all {@link FileToken} objects back
	 * to {@code File}s. As the files get modified, the provided synchronizer can be used to push the changes back to
	 * the original token sender.
	 *
	 * @param translator           An object that allows translation of file tokens back to files.
	 * @param synchronizer         In case the files corresponding to the token get changed, and this change has to be propagated
	 * @param filesThatShouldExist A set to which the translation would add all files that are supposed to exist on the receiver
	 *                             (they existed on the sender). If null, a new set is created + the existence of the files is checked at the end. If the
	 *                             files are missing, the function will wait up to 2 minutes for them to appear, after which an exception will be thrown.
	 */
	void translateOnReceiver(ReceiverTokenTranslator translator, FileTokenSynchronizer synchronizer, @Nullable Set<File> filesThatShouldExist);

	/**
	 * This method must be called at some point on the receiver side. The call of this method
	 * will synchronize the FileToken objects, between the system that creates this FileTokenHolder and the system that
	 * receives it.
	 * <p/>
	 * There are two main uses:
	 * <ul>
	 * <li>Work packet specifying a place where to put a newly created file. Once the file gets created on the receiver,
	 * it needs to be uploaded to the sender using {@link FileHolder#uploadAndWait}
	 * <li>Result of an operation containing a link to a file. The file needs to be downloaded on the target system.
	 * </ul>
	 */
	void synchronizeFileTokensOnReceiver();
}
