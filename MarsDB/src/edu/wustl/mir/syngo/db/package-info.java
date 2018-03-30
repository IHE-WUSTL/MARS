/**
 * Classes used to query the Syngo RIS DB. All access is made read-only without
 * locking, as we never modify this DB. The classes represent ORM "objects"
 * which correspond to informal views of the Syngo data convenient for us. For
 * details on the Syngo DB see the Syngo WorkFlow DB documentation in the
 * <a href="https://mirgforge.wustl.edu/gf/project/mars/docman/admin/?subdir=42">
 * Mars GForge</a>
 */
package edu.wustl.mir.syngo.db;