package com.quietanswerer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quietanswerer.App
import com.quietanswerer.audio.canChangeDnd
import com.quietanswerer.core.Prefs
import com.quietanswerer.data.db.IncomingCall
import com.quietanswerer.data.db.Reply
import com.quietanswerer.data.db.Status
import com.quietanswerer.data.db.Vip
import com.quietanswerer.phone.ContactFinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class VipItem(val vip: Vip, val name: String, val thumbnailUri: String?, val exists: Boolean)

data class UiState(
    val sessionOpen: Boolean = false,
    val canMute: Boolean = true,
    val soundSuppressed: Boolean = false,
    val themeMode: Int = Prefs.THEME_SYSTEM,
    val statuses: List<Status> = emptyList(),
    val currentStatusId: Long = -1L,
    val calls: List<IncomingCall> = emptyList(),
    val repliesByCall: Map<Long, List<Reply>> = emptyMap(),
    val vips: List<VipItem> = emptyList(),
    val autoStopAt: Long = -1L,
    val endCalls: Boolean = true,
    val contactRequired: Boolean = true,
    val frequencyMin: Int = 15,
    val muteMusic: Boolean = true,
    val muteNotifications: Boolean = true,
    val muteSystem: Boolean = true,
    val muteAlarm: Boolean = false,
    val callCount: Int = 0,
    val replyCount: Int = 0
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val di = (app as App).di

    private val _state = MutableStateFlow(
        UiState(
            soundSuppressed = Prefs.soundSuppressed(app),
            themeMode = Prefs.themeMode(app)
        )
    )
    val state: StateFlow<UiState> = _state

    // Навигация по вкладкам через external intents (уведомление/виджет передают EXTRA_TAB).
    private val _tabRequest = MutableStateFlow(-1)
    val tabRequest: StateFlow<Int> = _tabRequest.asStateFlow()

    fun requestTab(tab: Int) {
        if (tab >= 0) _tabRequest.value = tab
    }

    init {
        viewModelScope.launch {
            combine(
                di.repository.observeOpenSession(),
                di.repository.observeStatuses(),
                di.repository.observeCallCount(),
                di.repository.observeReplyCount()
            ) { session, statuses, callCount, replyCount ->
                UiState(
                    sessionOpen = session != null,
                    canMute = canChangeDnd(getApplication()),
                    soundSuppressed = Prefs.soundSuppressed(getApplication()),
                    statuses = statuses,
                    currentStatusId = Prefs.currentStatusId(getApplication()),
                    calls = _state.value.calls,
                    repliesByCall = _state.value.repliesByCall,
                    vips = _state.value.vips,
                    autoStopAt = Prefs.autoStopAt(getApplication()),
                    endCalls = Prefs.endCalls(getApplication()),
                    contactRequired = Prefs.contactRequired(getApplication()),
                    frequencyMin = Prefs.frequencyMinutes(getApplication()),
                    muteMusic = Prefs.muteMusic(getApplication()),
                    muteNotifications = Prefs.muteNotifications(getApplication()),
                    muteSystem = Prefs.muteSystem(getApplication()),
                    muteAlarm = Prefs.muteAlarm(getApplication()),
                    themeMode = Prefs.themeMode(getApplication()),
                    callCount = callCount,
                    replyCount = replyCount
                )
            }.collect { _state.value = it }
        }

        viewModelScope.launch {
            di.repository.observeOpenSession()
                .flatMapLatest { session ->
                    if (session == null) flowOf(emptyList())
                    else di.repository.observeCalls(session.id)
                }
                .collect { calls ->
                    _state.value = _state.value.copy(calls = calls)
                    loadReplies(calls)
                }
        }

        viewModelScope.launch {
            di.repository.observeVips().collect { vips -> refreshVips(vips) }
        }

        refreshEnvironment()
    }

    private fun loadReplies(calls: List<IncomingCall>) {
        viewModelScope.launch {
            val map = di.repository.repliesForCalls(calls.map { it.id })
            _state.value = _state.value.copy(repliesByCall = map.groupBy { it.incoming_call_id })
        }
    }

    private suspend fun refreshVips(vips: List<Vip>) {
        val items = vips.map { vip ->
            val contact = di.contactFinder.byContactId(vip.contact_id)
            VipItem(
                vip = vip,
                name = contact?.name?.takeIf { it.isNotBlank() } ?: "-",
                thumbnailUri = contact?.thumbnailUri,
                exists = contact != null
            )
        }
        _state.value = _state.value.copy(vips = items)
    }

    fun refreshEnvironment() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                canMute = canChangeDnd(getApplication()),
                soundSuppressed = Prefs.soundSuppressed(getApplication()),
                autoStopAt = Prefs.autoStopAt(getApplication())
            )
        }
    }

    // ---- Session / timer ----

    fun toggle() {
        viewModelScope.launch {
            di.sessionController.toggle()
            refreshEnvironment()
        }
    }

    fun setTimerMinutes(minutes: Int) {
        di.timerController.scheduleInMinutes(minutes)
        refreshEnvironment()
    }

    fun setTimerAt(epochMillis: Long) {
        di.timerController.scheduleAt(epochMillis)
        refreshEnvironment()
    }

    fun clearTimer() {
        di.timerController.clear()
        refreshEnvironment()
    }

    // ---- Statuses ----

    fun addStatus(text: String, name: String? = null): Boolean {
        if (text.isBlank()) return false
        fun fits(): Boolean {
            return try {
                android.telephony.SmsManager.getDefault().divideMessage(text).size == 1
            } catch (e: Exception) {
                text.length <= 160
            }
        }
        if (!fits()) return false
        val display = name?.takeIf { it.isNotBlank() } ?: text
        viewModelScope.launch {
            di.repository.statusDao.insert(Status(name = display, message = text))
            // Виджет показывает стрелки выбора варианта только когда вариантов больше одного.
            di.updater.refreshAll()
        }
        return true
    }

    fun setActiveStatus(id: Long) {
        Prefs.setCurrentStatusId(getApplication(), id)
        viewModelScope.launch {
            di.repository.statusDao.touch(id, System.currentTimeMillis())
            di.updater.refreshAll()
        }
        _state.value = _state.value.copy(currentStatusId = id)
    }

    fun duplicateStatus(id: Long) {
        viewModelScope.launch {
            di.repository.statusDao.byId(id)?.let {
                di.repository.statusDao.insert(
                    Status(name = it.name + " (2)", message = it.message)
                )
                di.updater.refreshAll()
            }
        }
    }

    fun removeStatus(id: Long) {
        viewModelScope.launch {
            di.repository.statusDao.byId(id)?.let { di.repository.statusDao.delete(it) }
            if (Prefs.currentStatusId(getApplication()) == id) {
                Prefs.setCurrentStatusId(getApplication(), -1)
            }
            di.updater.refreshAll()
        }
        _state.value = _state.value.copy(currentStatusId = if (Prefs.currentStatusId(getApplication()) == id) -1 else _state.value.currentStatusId)
    }

    fun clearLog() {
        viewModelScope.launch {
            di.repository.openSession()?.let { session ->
                di.repository.callDao.clearSession(session.id)
                di.updater.refreshAll()
            }
        }
    }

    // ---- VIP ----

    fun onContactPicked(uri: Uri) {
        viewModelScope.launch {
            val contact = resolvePicked(uri)
            if (contact != null) {
                val existing = di.repository.vipDao.byContact(contact.id)
                if (existing == null) {
                    di.repository.vipDao.insert(Vip(contact_id = contact.id, is_active = true))
                } else {
                    di.repository.vipDao.insert(existing.copy(is_active = true))
                }
            }
        }
    }

    private fun resolvePicked(uri: Uri): ContactFinder.Contact? {
        val cr = getApplication<Application>().contentResolver
        return try {
            var contactId: Long? = null
            var name = ""
            cr.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idIdx = c.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                    val nameIdx = c.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                    if (idIdx >= 0) {
                        contactId = c.getLong(idIdx)
                    } else {
                        val cidIdx = c.getColumnIndex("contact_id")
                        if (cidIdx >= 0) contactId = c.getLong(cidIdx)
                    }
                    if (nameIdx >= 0) name = c.getString(nameIdx).orEmpty()
                }
            }
            if (contactId == null) return null
            ContactFinder.Contact(contactId!!, name, null)
        } catch (e: Exception) {
            null
        }
    }

    fun setVipActive(vip: Vip, active: Boolean) {
        viewModelScope.launch {
            di.repository.vipDao.update(vip.copy(is_active = active))
        }
    }

    fun enableAllVips() = viewModelScope.launch { di.repository.vipDao.enableAll() }
    fun disableAllVips() = viewModelScope.launch { di.repository.vipDao.disableAll() }

    fun deleteVips(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { di.repository.vipDao.deleteByIds(ids) }
    }

    // ---- Settings ----

    fun setEndCalls(v: Boolean) {
        Prefs.setEndCalls(getApplication(), v)
        _state.value = _state.value.copy(endCalls = v)
        refreshEnvironment()
    }

    fun setContactRequired(v: Boolean) {
        Prefs.setContactRequired(getApplication(), v)
        _state.value = _state.value.copy(contactRequired = v)
        refreshEnvironment()
    }

    fun setFrequency(min: Int) {
        Prefs.setFrequencyMinutes(getApplication(), min)
        _state.value = _state.value.copy(frequencyMin = min)
        refreshEnvironment()
    }

    fun setMuteMusic(v: Boolean) {
        Prefs.setMuteMusic(getApplication(), v)
        _state.value = _state.value.copy(muteMusic = v)
        refreshEnvironment()
    }

    fun setMuteNotifications(v: Boolean) {
        Prefs.setMuteNotifications(getApplication(), v)
        _state.value = _state.value.copy(muteNotifications = v)
        refreshEnvironment()
    }

    fun setMuteSystem(v: Boolean) {
        Prefs.setMuteSystem(getApplication(), v)
        _state.value = _state.value.copy(muteSystem = v)
        refreshEnvironment()
    }

    fun setMuteAlarm(v: Boolean) {
        Prefs.setMuteAlarm(getApplication(), v)
        _state.value = _state.value.copy(muteAlarm = v)
        refreshEnvironment()
    }

    fun setThemeMode(mode: Int) {
        Prefs.setThemeMode(getApplication(), mode)
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun repliesFor(callId: Long): List<Reply> = _state.value.repliesByCall[callId] ?: emptyList()
}