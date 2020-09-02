package com.intumit.solr;

import com.hazelcast.core.Member;
import com.intumit.solr.servlet.HazelcastUtil;

/**
* A simple leader election implementation based on
* a Hazelcast cluster that simply elects the oldest
* member in the cluster as the leader.
* 
* @author Andreas Lindfalk
*/
public class HazelcastLeaderElection implements LeaderElection {

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isLeader() {

       // The first member in the set is the
       // oldest member, this is the one that
       // we always will point out as leader
       Member oldestMember = HazelcastUtil.getHzInstance().getCluster().getMembers().iterator().next();

       return oldestMember.localMember();
   }
} 