/**
 * Contains logic that preempts the Packet switch in order to handle packets that should not be routed to remote nodes.
 * This includes protocols like <tt>echo</tt> and anything addressed to the special <tt>peer.</tt> prefix.
 */
package com.sappenin.ilpv4.packetswitch.preemptors;