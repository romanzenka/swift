package edu.mayo.mprc.sequest.core;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class SequestSubmitStub implements SequestSubmitterInterface {
	private List<File> dtas;
	private SequestCallerInterface sequestCaller;

	SequestSubmitStub(final long maxLineLength, final long maxChunkSize, final int maxDtaFiles, final String paramsFileName, final String workingDir, final String tarName) {
		dtas = new ArrayList<File>();
	}

	@Override
	public void addDtaFile(final File file, final boolean forced) {
		dtas.add(file);
	}

	@Override
	public int getHowManyFiles() {
		return dtas.size();
	}

	@Override
	public void setExceptionThrown(final Throwable m) {

	}

	@Override
	public void forceSubmit() {

	}

	@Override
	public SequestCallerInterface getSequestCaller() {
		return sequestCaller;
	}

	@Override
	public void setSequestCaller(final SequestCallerInterface sequestCaller) {
		this.sequestCaller = sequestCaller;
	}
}
