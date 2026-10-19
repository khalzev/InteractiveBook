package com.interactivebook.app.ui.character

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
import com.interactivebook.app.ui.common.AssetImage
import com.interactivebook.app.ui.common.BookDialog
import com.interactivebook.app.ui.common.BookPageBackground
import com.interactivebook.app.ui.common.BookPageContent

private val InkColor = Color(0xFF3B2A1A)
private val GoldTextColor = Color(0xFFE8C766)
private val DarkBorderColor = Color(0xFF1A110A) // Темно-коричневый, почти черный
private val TextColor = Color(0xFFE8DBC0)

private const val ADVENTURER_SKILL_COUNT = 2
private val ArrowSize = 45.dp
private val SealSize = 80.dp

private data class CreationState(
    val currentClassIndex: Int = 0,
    val step: Int = 1,
    val selectedGender: Gender? = null,
    val chosenSkills: Set<SkillType> = emptySet(),
    val firstName: String = "",
    val lastName: String = ""
)

private data class ClassUiInfo(
    val archetype: Archetype,
    val displayName: String,
    val classImage: String,
    val tabImage: String,
    val description: String
)

private val classList = listOf(
    ClassUiInfo(
        Archetype.VETERAN, "Ветеран", "class_veteran.png", "tab_veteran.png",
        "Вы прошли не одну войну. Привыкли полагаться на силу, выносливость и собственные руки, а опасность научились встречать без лишних слов. Но годы сражений оставили после себя не только шрамы — некоторые воспоминания до сих пор не дают вам покоя. Много знает о людях, ещё больше о смерти."
    ),
    ClassUiInfo(
        Archetype.DETECTIVE, "Сыщик", "class_detective.png", "tab_detective.png",
        "Вы привыкли замечать то, что другие упускают, и задавать вопросы, на которые никто не хочет отвечать. Ловкость помогает вам пробираться туда, где чужакам не рады, а острый взгляд помогает находить следы, затерянные среди лжи. В Риверстоуне правда редко лежит на поверхности, и порой за неё приходится платить."
    ),
    ClassUiInfo(
        Archetype.HISTORIAN, "Историк", "class_historian.png", "tab_historian.png",
        "Вы ищите ответы в забытых хрониках, старых рукописях и преданиях, которые давно перестали интересовать живых. Старые записи способны пролить свет на загадки настоящего, если знать, где искать и как читать между строк. Но прошлое хранит не только знания, иногда оно оставляет после себя долги, которые некому забыть."
    ),
    ClassUiInfo(
        Archetype.INNKEEPER, "Трактирщик", "class_innkeeper.png", "tab_innkeeper.png",
        "Вы годами выслушивали пьяные исповеди, торговались с купцами, вышвыривали за порог буянов, которые забыли разницу между трактиром и бойцовской ямой, и наблюдали за теми, кто предпочитает оставаться в тени. Вы знаете, что нужное слово иногда стоит дороже хорошего клинка, а полезное знакомство открывает двери, запертые для остальных. Ваш трактир становился убежищем для одних и последней остановкой для других, а вы нередко знали больше, чем городские стражники. Но теперь привычный мир остался позади, и придётся самому искать дорогу вперёд."
    ),
    ClassUiInfo(
        Archetype.KNIGHT, "Рыцарь", "class_knight.png", "tab_knight.png",
        "Вы связали свою судьбу с честью, долгом и строгими правилами ордена. Сила и воспитание приучили вас стоять на своём, а имя и положение открывают двери, перед которыми другие вынуждены ждать. Но за стенами вашего ордена благородные слова мало чего стоят, если за ними не следует поступок."
    ),
    ClassUiInfo(
        Archetype.PHYSICIAN, "Врач", "class_physician.png", "tab_physician.png",
        "Посвятив себя изучению человеческого тела и борьбе со смертью, вы знаете цену человеческой жизни. Привыкли иметь дело с болью, кровью и отчаянием, не позволяя страху затуманить разум. Но даже самые глубокие познания не дают ответов на все вопросы."
    ),
    ClassUiInfo(
        Archetype.ADVENTURER, "Приключенец", "class_adventurer.png", "tab_adventurer.png",
        "Дорога давно стала вам привычнее дома. За плечами немало встреч, опасностей и решений, о которых не принято рассказывать. Вы не прославились ни выдающейся силой, ни учёной мудростью, зато умеете находить собственный путь и учиться тому, чего требует случай. В мире, полном опасностей и неожиданных встреч, это может оказаться главным преимуществом."
    )
)

@Composable
fun CharacterCreationScreen(
    onBack: () -> Unit = {},
    onCharacterReady: (Character) -> Unit
) {
    var state by remember { mutableStateOf(CreationState()) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val currentInfo = classList[state.currentClassIndex]
    val isAdventurer = currentInfo.archetype == Archetype.ADVENTURER

    val canGoForward = when (state.step) {
        1 -> true
        2 -> state.selectedGender != null
        3 -> isAdventurer && state.chosenSkills.size == ADVENTURER_SKILL_COUNT
        4 -> state.firstName.isNotBlank()
        else -> false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulsePrompt")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        BookPageBackground()

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 84.dp, start = 35.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            classList.forEachIndexed { index, classInfo ->
                val isSelected = index == state.currentClassIndex

                Box(
                    modifier = Modifier
                        .size(47.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (state.currentClassIndex != index) {
                                state = CreationState(currentClassIndex = index)
                            }
                        }
                ) {
                    AssetImage(
                        fileName = classInfo.tabImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(GoldTextColor.copy(alpha = 0.4f), Color.Transparent)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 2.dp,
                                    color = GoldTextColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }
        }

        BookPageContent {
            Box(modifier = Modifier.fillMaxSize()) {

                // Картинка с рамкой (обновленный цвет и толщина)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    AssetImage(
                        fileName = currentInfo.classImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 5.dp, // Увеличено в 2 раза
                                color = DarkBorderColor.copy(alpha = 0.85f), // Темно-коричневый
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                // Слой для света и затемнения
                if (state.step == 2) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
                        if (state.selectedGender == Gender.FEMALE) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(GoldTextColor.copy(alpha = 0.4f), Color.Transparent),
                                    center = Offset(size.width * 0.25f, size.height * 0.3f),
                                    radius = size.width * 0.45f
                                )
                            )
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                    startX = size.width * 0.4f,
                                    endX = size.width
                                )
                            )
                        } else if (state.selectedGender == Gender.MALE) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(GoldTextColor.copy(alpha = 0.4f), Color.Transparent),
                                    center = Offset(size.width * 0.75f, size.height * 0.3f),
                                    radius = size.width * 0.45f
                                )
                            )
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                                    startX = 0f,
                                    endX = size.width * 0.6f
                                )
                            )
                        }
                    }

                    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { state = state.copy(selectedGender = Gender.FEMALE) }
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { state = state.copy(selectedGender = Gender.MALE) }
                        )
                    }
                }

                Text(
                    text = currentInfo.displayName,
                    color = GoldTextColor,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 15.dp)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.80f)
                        .padding(bottom = 65.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.height(24.dp)) {
                        if (scrollState.canScrollBackward) {
                            Text(
                                text = "⌃",
                                color = TextColor.copy(alpha = 0.6f),
                                fontSize = 22.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                            .verticalScroll(scrollState),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (state.step) {
                            1 -> {
                                Text(
                                    text = currentInfo.description,
                                    color = TextColor,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            2 -> {
                                val archInfo = ArchetypeCatalog.infoFor(currentInfo.archetype)
                                val statsText = "Сила: ${statLabel(archInfo.stats.strength)}\n" +
                                        "Ловкость: ${statLabel(archInfo.stats.agility)}\n" +
                                        "Интеллект: ${statLabel(archInfo.stats.intellect)}"

                                val skills = if (state.selectedGender != null) {
                                    archInfo.skillsByGender?.get(state.selectedGender) ?: emptySet()
                                } else {
                                    emptySet()
                                }

                                val skillsText = if (state.selectedGender == null) {
                                    "Выберите пол персонажа (нажмите на героя)"
                                } else if (skills.isEmpty()) {
                                    "Навыки: будут выбраны на следующем шаге"
                                } else {
                                    "Навыки: " + skills.joinToString(", ") { skillLabel(it) }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = statsText,
                                        color = TextColor,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    if (state.selectedGender == null) {
                                        Text(
                                            text = skillsText,
                                            color = TextColor.copy(alpha = pulseAlpha),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Text(
                                            text = skillsText,
                                            color = TextColor,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            3 -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Выберите $ADVENTURER_SKILL_COUNT навыка",
                                        color = TextColor,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )

                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        SkillType.entries.forEach { skill ->
                                            val checked = state.chosenSkills.contains(skill)
                                            val textColor = if (checked) GoldTextColor else TextColor

                                            Text(
                                                text = (if (checked) "✓ " else "• ") + skillLabel(skill),
                                                color = textColor,
                                                fontSize = 16.sp,
                                                lineHeight = 20.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        state = when {
                                                            checked -> state.copy(chosenSkills = state.chosenSkills - skill)
                                                            state.chosenSkills.size < ADVENTURER_SKILL_COUNT ->
                                                                state.copy(chosenSkills = state.chosenSkills + skill)
                                                            else -> state
                                                        }
                                                    }
                                                    .padding(vertical = 0.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            4 -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val interactionSourceFirst = remember { MutableInteractionSource() }
                                    val isFocusedFirst by interactionSourceFirst.collectIsFocusedAsState()
                                    val borderColorFirst = if (isFocusedFirst) GoldTextColor else TextColor.copy(alpha = 0.5f)
                                    val fontFamily = MaterialTheme.typography.bodyLarge.fontFamily

                                    BasicTextField(
                                        value = state.firstName,
                                        onValueChange = { state = state.copy(firstName = it.replaceFirstChar { char -> char.uppercaseChar() }) },
                                        textStyle = TextStyle(
                                            textAlign = TextAlign.Center,
                                            fontSize = 14.sp,
                                            color = TextColor,
                                            fontFamily = fontFamily
                                        ),
                                        cursorBrush = SolidColor(TextColor), // Курсор цвета текста
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                        interactionSource = interactionSourceFirst,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .border(1.dp, borderColorFirst, RoundedCornerShape(4.dp)),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (state.firstName.isEmpty()) {
                                                    Text(
                                                        text = "Имя",
                                                        color = TextColor,
                                                        fontSize = 14.sp,
                                                        fontFamily = fontFamily, // Шрифт книги
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )

                                    val interactionSourceLast = remember { MutableInteractionSource() }
                                    val isFocusedLast by interactionSourceLast.collectIsFocusedAsState()
                                    val borderColorLast = if (isFocusedLast) GoldTextColor else TextColor.copy(alpha = 0.5f)

                                    BasicTextField(
                                        value = state.lastName,
                                        onValueChange = { state = state.copy(lastName = it.replaceFirstChar { char -> char.uppercaseChar() }) },
                                        textStyle = TextStyle(
                                            textAlign = TextAlign.Center,
                                            fontSize = 14.sp,
                                            color = TextColor,
                                            fontFamily = fontFamily
                                        ),
                                        cursorBrush = SolidColor(TextColor), // Курсор цвета текста
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                        interactionSource = interactionSourceLast,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .border(1.dp, borderColorLast, RoundedCornerShape(4.dp)),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (state.lastName.isEmpty()) {
                                                    Text(
                                                        text = "Фамилия",
                                                        color = TextColor,
                                                        fontSize = 14.sp,
                                                        fontFamily = fontFamily, // Шрифт книги
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.height(24.dp)) {
                        if (scrollState.canScrollForward) {
                            Text(
                                text = "⌄",
                                color = TextColor.copy(alpha = 0.6f),
                                fontSize = 22.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(y = (-20).dp, x = 16.dp)
                        .size(ArrowSize)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (state.step == 1) {
                                showExitDialog = true
                            } else {
                                val prevStep = when (state.step) {
                                    4 -> if (isAdventurer) 3 else 2
                                    3 -> 2
                                    else -> 1
                                }
                                state = state.copy(step = prevStep)
                            }
                        }
                ) {
                    AssetImage(
                        fileName = "arrow_left.png",
                        contentDescription = "Назад",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                if (state.step == 4) {
                    if (state.firstName.isNotBlank()) {
                        SealButton(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 20.dp)
                                .size(SealSize)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = (-20).dp, x = (-16).dp)
                            .size(ArrowSize)
                            .graphicsLayer { alpha = if (canGoForward) 1f else 0.4f }
                            .clickable(
                                enabled = canGoForward,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val nextStep = when (state.step) {
                                    1 -> 2
                                    2 -> if (isAdventurer) 3 else 4
                                    3 -> 4
                                    else -> 4
                                }
                                state = state.copy(step = nextStep)
                            }
                    ) {
                        AssetImage(
                            fileName = "arrow_right.png",
                            contentDescription = "Вперед",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        BookDialog(
            onDismissRequest = { showExitDialog = false },
            title = "Покинуть создание?",
            message = "Вы хотите покинуть страницу выбора персонажа.",
            confirmLabel = "Да",
            onConfirm = {
                showExitDialog = false
                onBack()
            },
            dismissLabel = "Отмена",
            onDismiss = { showExitDialog = false }
        )
    }

    if (showConfirmDialog) {
        BookDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = "Начать приключение?",
            message = "Точно ли вы готовы выбрать этого персонажа и начать приключение?",
            confirmLabel = "Да",
            onConfirm = {
                showConfirmDialog = false
                val info = ArchetypeCatalog.infoFor(currentInfo.archetype)
                val skills = if (isAdventurer) state.chosenSkills else
                    (info.skillsByGender?.get(state.selectedGender!!) ?: emptySet())

                onCharacterReady(
                    Character(
                        firstName = state.firstName.trim(),
                        lastName = state.lastName.trim(),
                        gender = state.selectedGender!!,
                        archetype = currentInfo.archetype,
                        stats = info.stats,
                        skills = skills
                    )
                )
            },
            dismissLabel = "Отмена",
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
private fun SealButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val imageFile = if (isPressed) "seal_pressed.png" else "seal_ready.png"

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        AssetImage(
            fileName = imageFile,
            contentDescription = "Подтвердить",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

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