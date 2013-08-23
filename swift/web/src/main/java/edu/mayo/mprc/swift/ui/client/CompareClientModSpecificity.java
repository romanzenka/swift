package edu.mayo.mprc.swift.ui.client;

import edu.mayo.mprc.swift.ui.client.rpc.ClientModSpecificity;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.io.Serializable;
import java.util.Comparator;

public final class CompareClientModSpecificity implements Comparator<ClientValue>, Serializable {
	private static final long serialVersionUID = 20101221L;

	@Override
	public int compare(final ClientValue o1, final ClientValue o2) {
		final ClientModSpecificity first = ClientModSpecificity.cast(o1);
		final ClientModSpecificity second = ClientModSpecificity.cast(o2);
		return first.getName().compareTo(second.getName());
	}
}
