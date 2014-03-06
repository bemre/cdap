==================================
Multi-Datacenter High Availability
==================================

When running across multiple datacenters, Loom can be configured to be resilient to datacenter failures. This document describes the recommended configuration 
for setting up Loom for HA across multiple datacenters. Together with :doc:`Datacenter High Availability <data-center-bcp>`, this setup provides for a comprehensive plan for Loom HA.

In this setup, Loom runs in active mode in all datacenters (Hot-Hot). In case of a datacenter failure, traffic from the failed datacenter will be automatically routed to other datacenters by the load balancer. This ensures that service is not affected on a datacenter failure.

A couple of things need to be considered when configuring Loom to run across multiple datacenters for HA-

* As discussed in the previous section, all components of Loom, except for database, either deal with local data or are stateless. The most important part of the HA setup is to share the data across datacenters in a consistent manner. HA configuration setup for multi-datacenter mostly depends on how the database is setup as discussed in the next sections.
* Since Loom Servers across all datacenters run in Hot-Hot mode, we have to make sure that they do not conflict while creating cluster IDs. The ID space needs to be partitioned amongst the Loom Servers. This can be done using ``loom.ids.start.num`` and ``loom.ids.increment.by`` server config parameters. For more information on the config parameters see :doc:`Server Configuration </guide/admin/server-config>` section. Also note that Loom Servers in a datacenter can share the same ID space.



We discuss two possible multi-datacenter HA configurations for Loom in the next sections. 
Note that the second option - HA with Custom Replication, is still work in progress. 

.. toctree::
   :maxdepth: 2

   option1/index
   option4/index
