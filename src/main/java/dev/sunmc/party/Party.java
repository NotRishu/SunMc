package dev.sunmc.party;

import java.util.*;

public class Party {

    private final UUID leader;
    private final List<UUID> members = new ArrayList<>();
    private final List<UUID> invited = new ArrayList<>();

    public Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public boolean isInvited(UUID uuid) { return invited.contains(uuid); }
    public void addMember(UUID uuid) { members.add(uuid); invited.remove(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public void invite(UUID uuid) { invited.add(uuid); }
    public void revokeInvite(UUID uuid) { invited.remove(uuid); }
    public int size() { return members.size(); }

    public UUID getLeader() { return leader; }
    public List<UUID> getMembers() { return Collections.unmodifiableList(members); }

    /** Split members into two even teams */
    public List<List<UUID>> split() {
        List<UUID> shuffled = new ArrayList<>(members);
        Collections.shuffle(shuffled);
        int half = shuffled.size() / 2;
        return List.of(shuffled.subList(0, half), shuffled.subList(half, shuffled.size()));
    }
}
