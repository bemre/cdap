:orphan:

.. _faq_toplevel:

.. index::
   single: FAQ: Loom Administration
============================
Loom Administration
============================

What operations are only available to the admin versus other users?
-------------------------------------------------------------------

Administrators can access all clusters, whereas users can access only
clusters they own. Only Administrators have super-user operations such as adding, deleting, 
and editing cluster templates, providers, images, hardware types and services.

What happens to existing clusters when the template used to create them changes?
--------------------------------------------------------------------------------

Existing clusters are not affected by any changes to providers, image types, 
hardware types, services, or cluster templates that are made after the time of
cluster creation. Each cluster keeps a copy of the template state at the time of
creation.

Note that clusters created with the original template will always retain their old configurations. 
Ability to update the clusters with the modified template is not currently supported.

How can I write configuration settings that reference hostnames of other nodes in the cluster?
----------------------------------------------------------------------------------------------

Loom provides a set of macros that allow you to do this.  See :doc:`Macros</guide/admin/macros>` for more information. 


Can I configure clusters to delete themselves after some amount of time?
------------------------------------------------------------------------

Cluster templates can define a Lease Duration after which the cluster will be automatically deleted,
see Creating a Template section in :doc:`Administration User Interface </guide/admin/ui>` for more information.
