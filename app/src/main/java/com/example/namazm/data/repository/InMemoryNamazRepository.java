package com.example.namazm.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.namazm.data.local.TurkeyCitiesAssetDataSource;
import com.example.namazm.data.local.TurkeyDistrictsAssetDataSource;
import com.example.namazm.data.local.db.CachedPrayerTimeEntity;
import com.example.namazm.data.local.db.HadithDao;
import com.example.namazm.data.local.db.HadithEntity;
import com.example.namazm.data.local.db.PrayerTimeDao;
import com.example.namazm.data.model.CalendarOverview;
import com.example.namazm.data.model.City;
import com.example.namazm.data.model.District;
import com.example.namazm.data.model.FavoriteContent;
import com.example.namazm.data.model.HadithCollection;
import com.example.namazm.data.model.HadithOfTheDay;
import com.example.namazm.data.model.LocationSuggestion;
import com.example.namazm.data.model.NotificationSettingsState;
import com.example.namazm.data.model.PrayerNotificationConfig;
import com.example.namazm.data.model.PrayerSlot;
import com.example.namazm.data.model.PrayerTime;
import com.example.namazm.data.model.PrayerTimesOverview;
import com.example.namazm.data.model.RamadanDay;
import com.example.namazm.data.model.SettingsState;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InMemoryNamazRepository implements NamazRepository {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LAST_UPDATED_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"));
    private static final String PREFS_NAME = "hadith_prefs";
    private static final String PREF_INSTALL_SEED = "install_seed";

    private final List<City> cities;
    private final List<District> districts;
    private final List<HadithOfTheDay> dayContents;
    private final List<HadithCollection> hadithCollections;
    private final Map<String, FavoriteContent> favorites = new LinkedHashMap<>();
    private final PrayerTimeDao prayerTimeDao;
    private final HadithDao hadithDao;
    private final int installSeed;

    private String selectedCityName = "";
    private String selectedDistrictName = "";
    private String selectedLocationLabel = "";

    private boolean notificationsEnabled = true;
    private int notificationOffsetMinutes = 10;
    private String themeMode = SettingsState.THEME_SYSTEM;
    private String dataSource = SettingsState.SOURCE_DIYANET;

    private boolean firstLaunchCitySelectionRequired = true;
    private boolean offlineMode = true;
    private String lastUpdatedLabel = "Son güncelleme: yok";
    private List<PrayerSlot> todaySlots;

    private NotificationSettingsState notificationSettings = NotificationSettingsState.defaultState();

    public InMemoryNamazRepository(
            Context context,
            TurkeyCitiesAssetDataSource cityDataSource,
            TurkeyDistrictsAssetDataSource districtDataSource,
            PrayerTimeDao prayerTimeDao,
            HadithDao hadithDao
    ) {
        this.cities = cityDataSource.loadCities();
        this.districts = districtDataSource.loadDistricts();
        this.prayerTimeDao = prayerTimeDao;
        this.hadithDao = hadithDao;
        this.todaySlots = defaultPrayerSlots();

        this.dayContents = loadHadithLibrary();
        this.hadithCollections = buildHadithCollections();
        this.installSeed = resolveInstallSeed(context.getApplicationContext());

        HadithOfTheDay starter = getDailyHadith(0);
        String starterSummary = starter.getShortText().isEmpty()
                ? shorten(starter.getText(), 46)
                : starter.getShortText();
        FavoriteContent starterFavorite = new FavoriteContent(
                starter.getId(),
                starter.getDayLabel(),
                starter.getTitle(),
                starterSummary,
                starter.getText(),
                starter.getSource()
        );
        favorites.put(starterFavorite.getId(), starterFavorite);
    }

    @Override
    public synchronized PrayerTimesOverview getPrayerTimesOverview() {
        String cityLabel = selectedLocationLabel.isEmpty()
                ? (selectedCityName.isEmpty() ? "İl seçin" : selectedCityName)
                : selectedLocationLabel;

        boolean cityRequired = firstLaunchCitySelectionRequired || selectedCityName.isEmpty();

        NextPrayerState next = computeNextPrayer(todaySlots);
        List<PrayerSlot> slotWithActive = buildSlotsWithActive(todaySlots, next.nextPrayerName);

        return new PrayerTimesOverview(
                cityLabel,
                next.nextPrayerName,
                next.nextPrayerTime,
                next.remaining,
                next.progress,
                notificationsEnabled,
                cityRequired,
                offlineMode,
                lastUpdatedLabel,
                slotWithActive
        );
    }

    @Override
    public synchronized CalendarOverview getCalendarOverview() {
        List<PrayerTime> monthly = Arrays.asList(
                new PrayerTime("01 Şub Cts", "05:43", "07:08", "13:14", "16:33", "19:10", "20:28"),
                new PrayerTime("02 Şub Paz", "05:42", "07:07", "13:15", "16:34", "19:11", "20:29"),
                new PrayerTime("03 Şub Pzt", "05:40", "07:05", "13:15", "16:35", "19:13", "20:31"),
                new PrayerTime("04 Şub Sal", "05:39", "07:04", "13:16", "16:36", "19:14", "20:32"),
                new PrayerTime("05 Şub Çar", "05:37", "07:02", "13:16", "16:37", "19:16", "20:34")
        );

        List<RamadanDay> ramadan = Arrays.asList(
                new RamadanDay("1 Ramazan", "05:31", "18:59"),
                new RamadanDay("2 Ramazan", "05:29", "19:00"),
                new RamadanDay("3 Ramazan", "05:28", "19:01"),
                new RamadanDay("4 Ramazan", "05:26", "19:03"),
                new RamadanDay("5 Ramazan", "05:24", "19:04")
        );

        String sahur = findSlotTime(todaySlots, PrayerNotificationConfig.KEY_IMSAK, "05:21");
        String iftar = findSlotTime(todaySlots, PrayerNotificationConfig.KEY_AKSAM, "19:06");

        String cityLabel = getSelectedLocationLabel().isEmpty() ? "İstanbul" : getSelectedLocationLabel();

        return new CalendarOverview(
                cityLabel,
                "Şubat 2026",
                true,
                "Bugün Sahur'a Kalan",
                computeNextPrayer(todaySlots).remaining,
                sahur,
                iftar,
                monthly,
                ramadan
        );
    }

    @Override
    public synchronized HadithOfTheDay getDailyHadith(int dayOffset) {
        if (dayContents.isEmpty()) {
            return new HadithOfTheDay(
                    "fallback",
                    dayLabel(dayOffset),
                    "Günün Hadisi",
                    "Bugün için hadis içeriği bulunamadı.",
                    "Yerel içerik",
                    "Hadis"
            );
        }

        long dayNumber = LocalDate.now().toEpochDay() + dayOffset;
        int baseIndex = positiveMod((int) (dayNumber + installSeed), dayContents.size());
        HadithOfTheDay base = dayContents.get(baseIndex);

        return new HadithOfTheDay(
                base.getId(),
                dayLabel(dayOffset),
                base.getTitle(),
                base.getText(),
                base.getSource(),
                base.getContentType(),
                base.getBook(),
                base.getTopic(),
                base.getShortText()
        );
    }

    @Override
    public synchronized List<HadithCollection> getHadithCollections() {
        return new ArrayList<>(hadithCollections);
    }

    @Override
    public synchronized List<HadithCollection> searchHadithCollections(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return getHadithCollections();
        }

        List<HadithCollection> results = new ArrayList<>();
        for (HadithCollection item : hadithCollections) {
            String joined = item.getTitle() + " " + item.getDescription() + " " + item.getTopic();
            if (normalize(joined).contains(normalized)) {
                results.add(item);
            }
        }
        return results;
    }

    @Override
    public synchronized boolean isFavorite(String contentId) {
        return favorites.containsKey(contentId);
    }

    @Override
    public synchronized void toggleFavorite(HadithOfTheDay content) {
        if (content == null) {
            return;
        }

        if (favorites.containsKey(content.getId())) {
            favorites.remove(content.getId());
            return;
        }

        String summary = content.getText().length() > 46
                ? content.getText().substring(0, 46) + "..."
                : content.getText();

        FavoriteContent favorite = new FavoriteContent(
                content.getId(),
                content.getDayLabel(),
                content.getTitle(),
                summary,
                content.getText(),
                content.getSource()
        );
        favorites.put(favorite.getId(), favorite);
    }

    @Override
    public synchronized List<FavoriteContent> getFavorites() {
        return new ArrayList<>(favorites.values());
    }

    @Override
    public synchronized FavoriteContent getFavoriteById(String id) {
        FavoriteContent favorite = favorites.get(id);
        if (favorite != null) {
            return favorite;
        }

        HadithOfTheDay today = getDailyHadith(0);
        return new FavoriteContent(
                today.getId(),
                today.getDayLabel(),
                today.getTitle(),
                today.getText(),
                today.getText(),
                today.getSource()
        );
    }

    @Override
    public synchronized SettingsState getSettingsState() {
        List<String> cityNames = new ArrayList<>();
        for (City city : cities) {
            cityNames.add(city.getName());
        }

        Collections.sort(cityNames);

        return new SettingsState(
                notificationsEnabled,
                getSelectedLocationLabel(),
                notificationOffsetMinutes,
                themeMode,
                dataSource,
                cityNames
        );
    }

    @Override
    public synchronized void updateSettings(
            String cityName,
            boolean notificationsEnabled,
            int notificationOffsetMinutes,
            String themeMode,
            String dataSource
    ) {
        if (cityName != null && !cityName.trim().isEmpty()) {
            selectCity(cityName.trim());
        }
        this.notificationsEnabled = notificationsEnabled;
        this.notificationOffsetMinutes = notificationOffsetMinutes;
        this.themeMode = themeMode;
        this.dataSource = dataSource;
    }

    @Override
    public synchronized List<City> getCities() {
        return new ArrayList<>(cities);
    }

    @Override
    public synchronized List<City> searchCities(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return getCities();
        }

        List<City> result = new ArrayList<>();
        for (City city : cities) {
            if (normalize(city.getName()).contains(normalized)) {
                result.add(city);
            }
        }
        return result;
    }

    @Override
    public synchronized void selectCity(String cityName) {
        selectedCityName = cityName;
        selectedDistrictName = "";
        selectedLocationLabel = cityName;
        firstLaunchCitySelectionRequired = false;
    }

    @Override
    public synchronized String getSelectedCityName() {
        return selectedCityName;
    }

    @Override
    public synchronized String getSelectedLocationLabel() {
        if (!selectedLocationLabel.isEmpty()) {
            return selectedLocationLabel;
        }
        return selectedCityName;
    }

    @Override
    public synchronized City findCityByName(String rawCityName) {
        if (rawCityName == null || rawCityName.trim().isEmpty()) {
            return null;
        }

        String normalized = normalize(rawCityName);
        for (City city : cities) {
            String cityNormalized = normalize(city.getName());
            if (cityNormalized.equals(normalized)
                    || normalized.contains(cityNormalized)
                    || cityNormalized.contains(normalized)) {
                return city;
            }
        }

        if (normalized.contains("icel")) {
            return findCityByName("Mersin");
        }
        if (normalized.contains("afyon")) {
            return findCityByName("Afyonkarahisar");
        }
        if (normalized.contains("kahramanmaras")) {
            return findCityByName("Kahramanmaraş");
        }
        return null;
    }

    @Override
    public synchronized List<LocationSuggestion> getLocationSuggestions() {
        List<LocationSuggestion> result = new ArrayList<>(cities.size() + districts.size());

        List<City> sortedCities = new ArrayList<>(cities);
        sortedCities.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (City city : sortedCities) {
            result.add(LocationSuggestion.fromCity(city));
        }

        List<District> sortedDistricts = new ArrayList<>(districts);
        sortedDistricts.sort((a, b) -> {
            int byCity = a.getCityName().compareToIgnoreCase(b.getCityName());
            if (byCity != 0) {
                return byCity;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (District district : sortedDistricts) {
            result.add(LocationSuggestion.fromDistrict(district));
        }
        return result;
    }

    @Override
    public synchronized List<LocationSuggestion> searchLocations(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return getLocationSuggestions();
        }

        List<LocationSuggestion> priority = new ArrayList<>();
        List<LocationSuggestion> secondary = new ArrayList<>();

        for (City city : cities) {
            String cityNormalized = normalize(city.getName());
            if (cityNormalized.contains(normalized)) {
                LocationSuggestion suggestion = LocationSuggestion.fromCity(city);
                if (cityNormalized.startsWith(normalized)) {
                    priority.add(suggestion);
                } else {
                    secondary.add(suggestion);
                }
            }
        }

        for (District district : districts) {
            String districtNormalized = normalize(district.getName());
            String cityNormalized = normalize(district.getCityName());
            if (districtNormalized.contains(normalized) || cityNormalized.contains(normalized)) {
                LocationSuggestion suggestion = LocationSuggestion.fromDistrict(district);
                if (districtNormalized.startsWith(normalized) || cityNormalized.startsWith(normalized)) {
                    priority.add(suggestion);
                } else {
                    secondary.add(suggestion);
                }
            }
        }

        priority.sort((a, b) -> a.getSelectionLabel().compareToIgnoreCase(b.getSelectionLabel()));
        secondary.sort((a, b) -> a.getSelectionLabel().compareToIgnoreCase(b.getSelectionLabel()));

        List<LocationSuggestion> result = new ArrayList<>(priority.size() + secondary.size());
        result.addAll(priority);
        result.addAll(secondary);
        return result;
    }

    @Override
    public synchronized LocationSuggestion findLocationByName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return null;
        }

        LocationSuggestion direct = findLocationByNameInternal(rawName);
        if (direct != null) {
            return direct;
        }

        String[] parts = rawName.split("[/,]");
        for (String part : parts) {
            LocationSuggestion tokenMatch = findLocationByNameInternal(part);
            if (tokenMatch != null) {
                return tokenMatch;
            }
        }

        return null;
    }

    private LocationSuggestion findLocationByNameInternal(String rawName) {
        String normalized = normalize(rawName);
        if (normalized.isEmpty()) {
            return null;
        }

        for (District district : districts) {
            String districtNormalized = normalize(district.getName());
            if (districtNormalized.equals(normalized)
                    || normalized.contains(districtNormalized)
                    || districtNormalized.contains(normalized)) {
                return LocationSuggestion.fromDistrict(district);
            }
        }

        City city = findCityByName(rawName);
        if (city != null) {
            return LocationSuggestion.fromCity(city);
        }

        return null;
    }

    @Override
    public synchronized void selectLocation(LocationSuggestion suggestion) {
        if (suggestion == null) {
            return;
        }

        selectedCityName = suggestion.getCityName();
        selectedDistrictName = suggestion.isDistrict() ? suggestion.getDistrictName() : "";
        selectedLocationLabel = suggestion.getSelectionLabel();
        firstLaunchCitySelectionRequired = false;
    }

    @Override
    public synchronized City findNearestCity(double latitude, double longitude) {
        City nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (City city : cities) {
            if (!city.hasCoordinates()) {
                continue;
            }
            double distance = haversine(latitude, longitude, city.getLatitude(), city.getLongitude());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = city;
            }
        }

        if (nearest != null) {
            return nearest;
        }

        City fallback = findCityByName("İstanbul");
        if (fallback != null) {
            return fallback;
        }
        return cities.isEmpty() ? null : cities.get(0);
    }

    @Override
    public synchronized boolean restorePrayerTimesFromCache(String cityName, String dateIso) {
        if (prayerTimeDao == null || cityName == null || cityName.trim().isEmpty()) {
            return false;
        }

        CachedPrayerTimeEntity entity = prayerTimeDao.getByCityAndDate(cityName, dateIso);
        if (entity == null) {
            return false;
        }

        List<PrayerSlot> slots = mapEntityToSlots(entity);
        applyPrayerTimes(cityName, slots, true);
        lastUpdatedLabel = "Son güncelleme: " + dateIso + " (önbellek)";
        return true;
    }

    @Override
    public synchronized void savePrayerTimesToCache(
            String cityName,
            String dateIso,
            List<PrayerSlot> slots,
            boolean offlineMode
    ) {
        if (cityName == null || cityName.trim().isEmpty() || slots == null || slots.isEmpty()) {
            return;
        }

        applyPrayerTimes(cityName, slots, offlineMode);
        if (prayerTimeDao == null) {
            return;
        }

        CachedPrayerTimeEntity entity = new CachedPrayerTimeEntity(
                cacheId(cityName, dateIso),
                cityName,
                dateIso,
                findSlotTime(slots, PrayerNotificationConfig.KEY_IMSAK, "05:30"),
                findSlotTime(slots, PrayerNotificationConfig.KEY_GUNES, "07:00"),
                findSlotTime(slots, PrayerNotificationConfig.KEY_OGLE, "13:10"),
                findSlotTime(slots, PrayerNotificationConfig.KEY_IKINDI, "16:30"),
                findSlotTime(slots, PrayerNotificationConfig.KEY_AKSAM, "19:00"),
                findSlotTime(slots, PrayerNotificationConfig.KEY_YATSI, "20:20")
        );
        prayerTimeDao.insertAll(Collections.singletonList(entity));
    }

    @Override
    public synchronized NotificationSettingsState getNotificationSettings() {
        return notificationSettings;
    }

    @Override
    public synchronized void updateNotificationSettings(NotificationSettingsState state) {
        if (state == null) {
            return;
        }

        notificationSettings = state;
        notificationsEnabled = state.isPrayerNotificationsEnabled();

        int defaultOffset = 5;
        for (PrayerNotificationConfig config : state.getPrayerConfigs()) {
            if (config.isEnabled()) {
                defaultOffset = config.getOffsetMinutes();
                break;
            }
        }
        notificationOffsetMinutes = defaultOffset;
    }

    private void applyPrayerTimes(String cityName, List<PrayerSlot> slots, boolean offlineMode) {
        if (!selectedCityName.equals(cityName)) {
            selectedDistrictName = "";
            selectedLocationLabel = cityName;
        }

        selectedCityName = cityName;
        firstLaunchCitySelectionRequired = false;
        this.offlineMode = offlineMode;
        this.todaySlots = sanitizeSlots(slots);
        this.lastUpdatedLabel = "Son güncelleme: " + LocalDateTime.now().format(LAST_UPDATED_FORMATTER);
    }

    private List<PrayerSlot> sanitizeSlots(List<PrayerSlot> slots) {
        List<PrayerSlot> result = new ArrayList<>();
        for (PrayerSlot slot : slots) {
            if (slot == null) {
                continue;
            }
            result.add(new PrayerSlot(slot.getName(), normalizeTime(slot.getTime()), false));
        }
        if (!result.isEmpty()) {
            return result;
        }
        return defaultPrayerSlots();
    }

    private List<PrayerSlot> mapEntityToSlots(CachedPrayerTimeEntity entity) {
        return Arrays.asList(
                new PrayerSlot("İmsak", normalizeTime(entity.getImsak()), false),
                new PrayerSlot("Güneş", normalizeTime(entity.getGunes()), false),
                new PrayerSlot("Öğle", normalizeTime(entity.getOgle()), false),
                new PrayerSlot("İkindi", normalizeTime(entity.getIkindi()), false),
                new PrayerSlot("Akşam", normalizeTime(entity.getAksam()), false),
                new PrayerSlot("Yatsı", normalizeTime(entity.getYatsi()), false)
        );
    }

    private String cacheId(String cityName, String dateIso) {
        return normalize(cityName) + "_" + dateIso;
    }

    private List<PrayerSlot> defaultPrayerSlots() {
        return Arrays.asList(
                new PrayerSlot("İmsak", "05:39", false),
                new PrayerSlot("Güneş", "07:02", false),
                new PrayerSlot("Öğle", "13:18", false),
                new PrayerSlot("İkindi", "16:44", false),
                new PrayerSlot("Akşam", "19:21", false),
                new PrayerSlot("Yatsı", "20:40", false)
        );
    }

    private List<HadithCollection> buildHadithCollections() {
        return Arrays.asList(
                new HadithCollection(
                        "collection_hadislerle_islam",
                        "Hadislerle İslam",
                        "Diyanet'in dijital hadis yayını. Konu bazlı ve okunaklı içerikler.",
                        "Diyanet",
                        "İman • İbadet • Ahlak",
                        "https://hadislerleislam.diyanet.gov.tr/"
                ),
                new HadithCollection(
                        "collection_hadis_portal",
                        "Diyanet Hadis Portalı",
                        "Hadis kaynaklarına ve konu başlıklarına hızlı erişim.",
                        "Diyanet",
                        "Arama • Konular",
                        "https://hadis.diyanet.gov.tr/"
                ),
                new HadithCollection(
                        "collection_diyanet_pdf",
                        "100 Hadis (PDF Kaynakları)",
                        "Diyanet yayınları üzerinden PDF koleksiyonlarına erişim.",
                        "Diyanet Yayın",
                        "PDF • Arşiv",
                        "https://yayin.diyanet.gov.tr/"
                )
        );
    }

    private List<HadithOfTheDay> loadHadithLibrary() {
        List<HadithEntity> seed = buildSeedHadithEntities();
        if (hadithDao == null) {
            return mapHadithEntities(seed);
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> countFuture = executor.submit(hadithDao::count);
            int count = countFuture.get();

            if (count == 0) {
                Future<?> insertFuture = executor.submit(() -> hadithDao.insertAll(seed));
                insertFuture.get();
            }

            Future<List<HadithEntity>> allFuture = executor.submit(hadithDao::getAll);
            List<HadithEntity> stored = allFuture.get();
            if (stored != null && !stored.isEmpty()) {
                return mapHadithEntities(stored);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
        } finally {
            executor.shutdown();
        }

        return mapHadithEntities(seed);
    }

    private List<HadithEntity> buildSeedHadithEntities() {
        return Arrays.asList(
                new HadithEntity(
                        "h001",
                        "Günün Hadisi",
                        "Merhamet etmeyene merhamet edilmez.",
                        "Buhari, Edeb 18",
                        "Hadis",
                        "Riyazü's-Salihin",
                        "Merhamet",
                        "Merhamet etmeyene merhamet edilmez."
                ),
                new HadithEntity(
                        "h002",
                        "Günün Hadisi",
                        "Kolaylaştırın, zorlaştırmayın; müjdeleyin, nefret ettirmeyin.",
                        "Buhari, İlim 12",
                        "Hadis",
                        "Riyazü's-Salihin",
                        "Tebliğ",
                        "Kolaylaştırın, zorlaştırmayın."
                ),
                new HadithEntity(
                        "h003",
                        "Günün Hadisi",
                        "Ameller niyetlere göredir.",
                        "Buhari, Bed'ü'l-vahy 1",
                        "Hadis",
                        "Riyazü's-Salihin",
                        "Niyet",
                        "Ameller niyetlere göredir."
                ),
                new HadithEntity(
                        "h004",
                        "Günün Hadisi",
                        "İnsanların en hayırlısı insanlara faydalı olandır.",
                        "Deylemi, Müsnedü'l-Firdevs",
                        "Hadis",
                        "Hadislerle İslam",
                        "Fayda",
                        "En hayırlı insan, insanlara faydalı olandır."
                ),
                new HadithEntity(
                        "h005",
                        "Günün Hadisi",
                        "Komşusu açken tok yatan bizden değildir.",
                        "Hâkim, el-Müstedrek 4/183",
                        "Hadis",
                        "Hadislerle İslam",
                        "Komşuluk",
                        "Komşusu açken tok yatan bizden değildir."
                ),
                new HadithEntity(
                        "h006",
                        "Günün Hadisi",
                        "Temizlik imanın yarısıdır.",
                        "Müslim, Taharet 1",
                        "Hadis",
                        "Riyazü's-Salihin",
                        "Temizlik",
                        "Temizlik, imanın yarısıdır."
                ),
                new HadithEntity(
                        "h007",
                        "Günün Hadisi",
                        "Dua ibadetin özüdür.",
                        "Tirmizi, Deavat 1",
                        "Hadis",
                        "Hadislerle İslam",
                        "Dua",
                        "Dua, ibadetin özüdür."
                ),
                new HadithEntity(
                        "h008",
                        "Günün Ayeti",
                        "Şüphesiz namaz, hayasızlıktan ve kötülükten alıkoyar.",
                        "Ankebut 29/45",
                        "Ayet",
                        "Kur'an-ı Kerim",
                        "Namaz",
                        "Namaz, hayasızlıktan ve kötülükten alıkoyar."
                ),
                new HadithEntity(
                        "h009",
                        "Günün Ayeti",
                        "Şüphesiz Allah sabredenlerle beraberdir.",
                        "Bakara 2/153",
                        "Ayet",
                        "Kur'an-ı Kerim",
                        "Sabır",
                        "Allah, sabredenlerle beraberdir."
                ),
                new HadithEntity(
                        "h010",
                        "Günün Duası",
                        "Rabbimiz! Bize göz aydınlığı olacak eşler ve nesiller bağışla.",
                        "Furkan 25/74",
                        "Dua",
                        "Kur'an-ı Kerim",
                        "Aile",
                        "Rabbimiz, bize hayırlı eşler ve nesiller bağışla."
                ),
                new HadithEntity(
                        "h011",
                        "Günün Hadisi",
                        "Güzel söz sadakadır.",
                        "Buhari, Edeb 34",
                        "Hadis",
                        "Riyazü's-Salihin",
                        "Ahlak",
                        "Güzel söz sadakadır."
                ),
                new HadithEntity(
                        "h012",
                        "Günün Hadisi",
                        "Mümin, elinden ve dilinden insanların emin olduğu kimsedir.",
                        "Tirmizi, İman 12",
                        "Hadis",
                        "Hadislerle İslam",
                        "Emanet",
                        "Mümin, insanların güven duyduğu kimsedir."
                )
        );
    }

    private List<HadithOfTheDay> mapHadithEntities(List<HadithEntity> entities) {
        List<HadithOfTheDay> result = new ArrayList<>();
        for (HadithEntity entity : entities) {
            if (entity == null) {
                continue;
            }
            result.add(new HadithOfTheDay(
                    entity.getId(),
                    "",
                    entity.getTitle(),
                    entity.getText(),
                    entity.getSource(),
                    entity.getContentType(),
                    entity.getBook(),
                    entity.getTopic(),
                    entity.getShortText()
            ));
        }
        return result;
    }

    private int resolveInstallSeed(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int existing = preferences.getInt(PREF_INSTALL_SEED, Integer.MIN_VALUE);
        if (existing != Integer.MIN_VALUE) {
            return existing;
        }

        long firstInstallTime = 0L;
        try {
            firstInstallTime = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .firstInstallTime;
        } catch (Exception ignored) {
            firstInstallTime = System.currentTimeMillis();
        }

        int generated = (int) ((firstInstallTime ^ (firstInstallTime >>> 32)) & Integer.MAX_VALUE);
        if (generated == 0) {
            generated = Math.abs(context.getPackageName().hashCode());
        }
        preferences.edit().putInt(PREF_INSTALL_SEED, generated).apply();
        return generated;
    }

    private String dayLabel(int dayOffset) {
        if (dayOffset == 0) {
            return "Bugün";
        }
        if (dayOffset == -1) {
            return "Dün";
        }
        if (dayOffset == 1) {
            return "Yarın";
        }
        if (dayOffset > 1) {
            return "+" + dayOffset + " Gün";
        }
        return dayOffset + " Gün";
    }

    private int positiveMod(int value, int modulo) {
        int raw = value % modulo;
        return raw < 0 ? raw + modulo : raw;
    }

    private String shorten(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen).trim() + "...";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String lower = value.toLowerCase(Locale.forLanguageTag("tr-TR"))
                .replace('ı', 'i')
                .replace('ğ', 'g')
                .replace('ü', 'u')
                .replace('ş', 's')
                .replace('ö', 'o')
                .replace('ç', 'c');

        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim();
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371d * c;
    }

    private NextPrayerState computeNextPrayer(List<PrayerSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return new NextPrayerState("Akşam", "19:21", "00:00:00", 0);
        }

        List<PrayerSlot> usable = sanitizeSlots(slots);
        LocalTime now = LocalTime.now();

        int nextIndex = 0;
        boolean found = false;
        for (int i = 0; i < usable.size(); i++) {
            LocalTime slotTime = parseOrDefault(usable.get(i).getTime(), LocalTime.MIDNIGHT);
            if (slotTime.isAfter(now)) {
                nextIndex = i;
                found = true;
                break;
            }
        }

        if (!found) {
            nextIndex = 0;
        }

        int previousIndex = nextIndex - 1;
        if (previousIndex < 0) {
            previousIndex = usable.size() - 1;
        }

        PrayerSlot nextSlot = usable.get(nextIndex);
        LocalTime nextTime = parseOrDefault(nextSlot.getTime(), LocalTime.MIDNIGHT);
        LocalTime prevTime = parseOrDefault(usable.get(previousIndex).getTime(), LocalTime.MIDNIGHT);

        Duration remainingDuration = Duration.between(now, nextTime);
        if (remainingDuration.isNegative() || remainingDuration.isZero()) {
            remainingDuration = remainingDuration.plusHours(24);
        }

        Duration totalDuration = Duration.between(prevTime, nextTime);
        if (totalDuration.isNegative() || totalDuration.isZero()) {
            totalDuration = totalDuration.plusHours(24);
        }

        Duration elapsed = totalDuration.minus(remainingDuration);
        if (elapsed.isNegative()) {
            elapsed = Duration.ZERO;
        }

        long totalSeconds = Math.max(totalDuration.getSeconds(), 1);
        long elapsedSeconds = Math.max(0, Math.min(elapsed.getSeconds(), totalSeconds));
        int progress = (int) ((elapsedSeconds * 100) / totalSeconds);

        return new NextPrayerState(
                nextSlot.getName(),
                nextSlot.getTime(),
                formatDuration(remainingDuration),
                Math.max(0, Math.min(progress, 100))
        );
    }

    private List<PrayerSlot> buildSlotsWithActive(List<PrayerSlot> slots, String activeName) {
        List<PrayerSlot> result = new ArrayList<>();
        for (PrayerSlot slot : slots) {
            boolean isActive = slot.getName().equals(activeName);
            result.add(new PrayerSlot(slot.getName(), slot.getTime(), isActive));
        }
        return result;
    }

    private String normalizeTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "00:00";
        }
        String cleaned = value.trim();
        int spaceIndex = cleaned.indexOf(' ');
        if (spaceIndex > 0) {
            cleaned = cleaned.substring(0, spaceIndex);
        }
        int bracketIndex = cleaned.indexOf('(');
        if (bracketIndex > 0) {
            cleaned = cleaned.substring(0, bracketIndex).trim();
        }
        if (cleaned.matches("\\d{1,2}:\\d{2}")) {
            String[] parts = cleaned.split(":");
            return String.format(Locale.US, "%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return "00:00";
    }

    private LocalTime parseOrDefault(String value, LocalTime fallback) {
        try {
            return LocalTime.parse(normalizeTime(value), TIME_FORMATTER);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private String findSlotTime(List<PrayerSlot> slots, String prayerKey, String fallback) {
        for (PrayerSlot slot : slots) {
            String key = normalize(slot.getName());
            if (PrayerNotificationConfig.KEY_IMSAK.equals(prayerKey) && key.contains("imsak")) {
                return slot.getTime();
            }
            if (PrayerNotificationConfig.KEY_GUNES.equals(prayerKey) && key.contains("gunes")) {
                return slot.getTime();
            }
            if (PrayerNotificationConfig.KEY_OGLE.equals(prayerKey) && key.contains("ogle")) {
                return slot.getTime();
            }
            if (PrayerNotificationConfig.KEY_IKINDI.equals(prayerKey) && key.contains("ikindi")) {
                return slot.getTime();
            }
            if (PrayerNotificationConfig.KEY_AKSAM.equals(prayerKey) && key.contains("aksam")) {
                return slot.getTime();
            }
            if (PrayerNotificationConfig.KEY_YATSI.equals(prayerKey) && key.contains("yatsi")) {
                return slot.getTime();
            }
        }
        return fallback;
    }

    private static class NextPrayerState {
        final String nextPrayerName;
        final String nextPrayerTime;
        final String remaining;
        final int progress;

        NextPrayerState(String nextPrayerName, String nextPrayerTime, String remaining, int progress) {
            this.nextPrayerName = nextPrayerName;
            this.nextPrayerTime = nextPrayerTime;
            this.remaining = remaining;
            this.progress = progress;
        }
    }
}
