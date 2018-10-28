/**
 * <p>A java implementation of `ilp-plugin-mini-accounts`.</p>
 *
 * <p>ILP Plugin Mini-Accounts provides a way for many users to sign up for a connector without the connector modifying
 * its configuration. It is a type of multi-user plugin, which means that it internally implements an extremely
 * bare-bones connector.</p>
 *
 * </p>This plugin can be connected to with ilp-plugin-btp. Any secret can be used to authenticate; it is then hashed
 * and becomes your account identifier. This has the advantage of requiring no UI-based signup flow nor any database
 * storing usernames and passwords. It has the disadvantage that password recovery cannot be done, and only randomly
 * generated passwords should be used. Treat your credentials like you would treat a wallet secret for a
 * cryptocurrency.</p>
 *
 * @see "https://github.com/interledgerjs/ilp-plugin-virtual"
 */

package com.sappenin.ilpv4.plugins.miniaccounts;