/**
 * Given a JMS broker URI, file transfers between systems are done via socket connections.
 * Systems listen to a queue named after the system unique id for file transfer requests.
 * Once the proper hand-shake process using the JMS means has been completed, files are transferred
 * using socket connections between the requesting and receiving systems.
 */
package edu.mayo.mprc.filesharing.jms;