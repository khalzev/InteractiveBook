package com.interactivebook.app.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interactivebook.app.core.model.Archetype
import com.interactivebook.app.core.model.ArchetypeCatalog
import com.interactivebook.app.core.model.Character
import com.interactivebook.app.core.model.Gender
import com.interactivebook.app.core.model.SkillType
import com.interactivebook.app.core.model.StatLevel
import com.interactivebook.app.core.model.Stats

private const val ADVENTURER_SKILL_COUNT = 2

/**
 * Мастер создания персонажа (разделы 9-15), 3 шага — или 4, если выбран Приключенец:
 * 1) класс (карусель по кругу, без бонусов)
 * 2) пол (с характеристиками и навыками класса)
 * 3) [только для Приключенца] выбор 2 навыков — отдельный шаг
 * 4) имя/фамилия
 * Далее — вступительная сцена игры (game_intro в DemoContent).
 */
private sealed interface CreationStep {
    data object ClassSelect : CreationStep
    data class GenderSelect(val archetype: Archetype) : CreationStep
    data class SkillSelect(val archetype: Archetype, val gender: Gender) : CreationStep
    data class NameEntry(val archetype: Archetype, val gender: Gender, val skills: Set<SkillType>) : CreationStep
}

@Composable
fun CharacterCreationScreen(onCharacterReady: (Character) -> Unit) {
    var step by remember { mutableStateOf<CreationStep>(CreationStep.ClassSelect) }

    when (val current = step) {
        is CreationStep.ClassSelect -> ClassCarouselStep(
            onNext = { archetype -> step = CreationStep.GenderSelect(archetype) }
        )

        is CreationStep.GenderSelect -> GenderSelectStep(
            archetype = current.archetype,
            onBack = { step = CreationStep.ClassSelect },
            onNext = { gender ->
                step = if (current.archetype == Archetype.ADVENTURER) {
                    CreationStep.SkillSelect(current.archetype, gender)
                } else {
                    val skills = ArchetypeCatalog.infoFor(current.archetype).skillsByGender?.get(gender) ?: emptySet()
                    CreationStep.NameEntry(current.archetype, gender, skills)
                }
            }
        )

        is CreationStep.SkillSelect -> SkillSelectStep(
            onBack = { step = CreationStep.GenderSelect(current.archetype) },
            onNext = { skills -> step = CreationStep.NameEntry(current.archetype, current.gender, skills) }
        )

        is CreationStep.NameEntry -> NameEntryStep(
            onBack = {
                step = if (current.archetype == Archetype.ADVENTURER) {
                    CreationStep.SkillSelect(current.archetype, current.gender)
                } else {
                    CreationStep.GenderSelect(current.archetype)
                }
            },
            onComplete = { firstName, lastName ->
                val info = ArchetypeCatalog.infoFor(current.archetype)
                onCharacterReady(
                    Character(
                        firstName = firstName,
                        lastName = lastName,
                        gender = current.gender,
                        archetype = current.archetype,
                        stats = info.stats,
                        skills = current.skills
                    )
                )
            }
        )
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    if (onBack != null) {
        TextButton(onClick = onBack) { Text("← Назад") }
    }
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(4.dp))
    Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
}

// ---------- Шаг 1: класс (карусель по кругу) ----------

@Composable
private fun ClassCarouselStep(onNext: (Archetype) -> Unit) {
    val archetypes = ArchetypeCatalog.all
    val pageCount = archetypes.size
    // Виртуально огромное число страниц + индекс по модулю реального количества классов —
    // так карусель крутится по кругу в обе стороны без начала и конца.
    val startPage = remember { (Int.MAX_VALUE / 2 / pageCount) * pageCount }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { Int.MAX_VALUE })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepHeader(title = "Выберите класс", subtitle = "Класс персонажа")

        // contentPadding оставляет по бокам видимые "срезы" соседних карточек —
        // ощущение кругового пролистывания, как и просили.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 56.dp),
            pageSpacing = 16.dp
        ) { page ->
            val info = archetypes[Math.floorMod(page, pageCount)]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = archetypeEmoji(info.archetype), fontSize = 80.sp)
                }
                Spacer(Modifier.height(20.dp))
                Text(info.displayName, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val current = Math.floorMod(pagerState.currentPage, pageCount)
            archetypes.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onNext(archetypes[Math.floorMod(pagerState.currentPage, pageCount)].archetype) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Выбрать", style = MaterialTheme.typography.titleLarge)
        }
    }
}

// ---------- Шаг 2: пол (с бонусами) ----------

@Composable
private fun GenderSelectStep(
    archetype: Archetype,
    onBack: () -> Unit,
    onNext: (Gender) -> Unit
) {
    val info = ArchetypeCatalog.infoFor(archetype)
    val isAdventurer = archetype == Archetype.ADVENTURER
    var selectedGender by remember { mutableStateOf<Gender?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        StepHeader(
            title = "${info.displayName}: выберите пол",
            subtitle = "Пол и характеристики",
            onBack = onBack
        )

        Gender.entries.forEach { gender ->
            val skillsForGender = if (isAdventurer) null else info.skillsByGender?.get(gender)
            GenderCard(
                gender = gender,
                archetype = archetype,
                stats = info.stats,
                skills = skillsForGender,
                selected = selectedGender == gender,
                onSelect = { selectedGender = gender }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { selectedGender?.let(onNext) },
            enabled = selectedGender != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Далее", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GenderCard(
    gender: Gender,
    archetype: Archetype,
    stats: Stats,
    skills: Set<SkillType>?,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(text = archetypeEmoji(archetype), fontSize = 30.sp)
                Text(
                    text = if (gender == Gender.MALE) "♂" else "♀",
                    modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = if (gender == Gender.MALE) "Мужской" else "Женский",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Сила ${statLabel(stats.strength)} · Ловкость ${statLabel(stats.agility)} · " +
                        "Интеллект ${statLabel(stats.intellect)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (skills != null) {
                        "Навыки: " + skills.joinToString(", ") { skillLabel(it) }
                    } else {
                        "Навыки выбираются на следующем шаге"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// ---------- Шаг 3 (только Приключенец): выбор навыков ----------

@Composable
private fun SkillSelectStep(
    onBack: () -> Unit,
    onNext: (Set<SkillType>) -> Unit
) {
    var chosen by remember { mutableStateOf(setOf<SkillType>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        StepHeader(
            title = "Выберите навыки",
            subtitle = "Ровно $ADVENTURER_SKILL_COUNT навыка (выбрано: ${chosen.size})",
            onBack = onBack
        )

        SkillType.entries.forEach { skill ->
            val checked = chosen.contains(skill)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        chosen = when {
                            checked -> chosen - skill
                            chosen.size < ADVENTURER_SKILL_COUNT -> chosen + skill
                            else -> chosen
                        }
                    }
                    .border(
                        width = if (checked) 2.dp else 1.dp,
                        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                color = if (checked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(skillLabel(skill), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(2.dp))
                    Text(skillDescription(skill), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = { onNext(chosen) },
            enabled = chosen.size == ADVENTURER_SKILL_COUNT,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Далее", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ---------- Финальный шаг: имя ----------

@Composable
private fun NameEntryStep(
    onBack: () -> Unit,
    onComplete: (String, String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    val canStart = firstName.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        StepHeader(title = "Имя героя", subtitle = "Имя и фамилия", onBack = onBack)

        Text("Имя", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = capitalizeFirst(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(Modifier.height(16.dp))

        Text("Фамилия", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = capitalizeFirst(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { onComplete(firstName.trim(), lastName.trim()) },
            enabled = canStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Начать игру", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun capitalizeFirst(value: String): String =
    if (value.isEmpty()) value else value.replaceFirstChar { it.uppercaseChar() }

private fun statLabel(level: StatLevel): String = when (level) {
    StatLevel.HIGH -> "+2"
    StatLevel.MEDIUM -> "0"
    StatLevel.LOW -> "-2"
}

private fun skillLabel(skill: SkillType): String = when (skill) {
    SkillType.ATHLETICS -> "Атлетика"
    SkillType.KNOWLEDGE -> "Знание"
    SkillType.ELOQUENCE -> "Красноречие"
    SkillType.SURVIVAL -> "Выживание"
    SkillType.OBSERVATION -> "Наблюдательность"
}

private fun skillDescription(skill: SkillType): String = when (skill) {
    SkillType.ATHLETICS -> "+3 к проверкам Силы, +2 к проверкам Ловкости"
    SkillType.KNOWLEDGE -> "+3 к проверкам на общие знания — история, религия, политика"
    SkillType.ELOQUENCE -> "+3 к убеждению, +2 к запугиванию"
    SkillType.SURVIVAL -> "+1 ко всем остальным проверкам (Атлетика, Знание, Красноречие, Наблюдательность)"
    SkillType.OBSERVATION -> "+3 к проверкам наблюдательности"
}

// Пока нет иллюстраций (раздел 81) — эмодзи как плейсхолдер образа класса.
private fun archetypeEmoji(archetype: Archetype): String = when (archetype) {
    Archetype.VETERAN -> "⚔️"
    Archetype.DETECTIVE -> "🔍"
    Archetype.HISTORIAN -> "📜"
    Archetype.INNKEEPER -> "🍺"
    Archetype.KNIGHT -> "🛡️"
    Archetype.PHYSICIAN -> "⚕️"
    Archetype.ADVENTURER -> "🎒"
}
