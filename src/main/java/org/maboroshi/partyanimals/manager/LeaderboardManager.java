package org.maboroshi.partyanimals.manager;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.manager.DatabaseManager.TopVoter;

public class LeaderboardManager {
    private final PartyAnimals plugin;
    private final DatabaseManager db;

    private final ConcurrentHashMap<String, List<TopVoter>> cache = new ConcurrentHashMap<>();

    public LeaderboardManager(PartyAnimals plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        startUpdateTask();
    }

    private void startUpdateTask() {
        Bukkit.getAsyncScheduler()
                .runAtFixedRate(
                        plugin,
                        (task) -> {
                            this.refreshLeaderboards();
                        },
                        0L,
                        300L,
                        TimeUnit.SECONDS);
    }

    public void refreshLeaderboards() {
        long now = System.currentTimeMillis();

        cache.put("alltime", db.getTopVoters(0, 10));

        cache.put("last_24h", db.getTopVoters(now - (24L * 60 * 60 * 1000), 10));
        cache.put("last_7d", db.getTopVoters(now - (7L * 24 * 60 * 60 * 1000), 10));
        cache.put("last_30d", db.getTopVoters(now - (30L * 24 * 60 * 60 * 1000), 10));

        Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);

        cache.put("daily", db.getTopVoters(cal.getTimeInMillis(), 10));

        Calendar weekCal = (java.util.Calendar) cal.clone();
        weekCal.set(java.util.Calendar.DAY_OF_WEEK, weekCal.getFirstDayOfWeek());
        cache.put("weekly", db.getTopVoters(weekCal.getTimeInMillis(), 10));

        Calendar monthCal = (java.util.Calendar) cal.clone();
        monthCal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cache.put("monthly", db.getTopVoters(monthCal.getTimeInMillis(), 10));

        Calendar yearCal = (java.util.Calendar) cal.clone();
        yearCal.set(java.util.Calendar.DAY_OF_YEAR, 1);
        cache.put("yearly", db.getTopVoters(yearCal.getTimeInMillis(), 10));
    }

    public TopVoter getTopVoter(String type, int rank) {
        List<TopVoter> list = cache.getOrDefault(type.toLowerCase(), Collections.emptyList());
        int index = rank - 1;

        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return new TopVoter("---", 0);
    }
}
