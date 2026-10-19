package com.interactivebook.app.content.scenes

import com.interactivebook.app.content.chapters.Chapter1
import com.interactivebook.app.core.conditions.Condition
import com.interactivebook.app.core.effects.Effect
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.core.model.ActionConfirmation
import com.interactivebook.app.core.model.Archetype
import com.interactivebook.app.core.model.PartTitle
import com.interactivebook.app.core.model.Scene
import com.interactivebook.app.core.model.TextVariant

/**
 * "Тайны Риверстоуна" — Книга Первая «Город у реки», Пролог.
 */
object DemoContent {

    /** Единый текст подтверждения для «Пропустить пролог» — ведет на 16 страницу */
    private fun skipPrologueAction(pageId: String) = Action(
        id = "${pageId}_skip_prologue",
        text = "Пропустить пролог",
        confirmation = ActionConfirmation(
            title = "Пропустить пролог?",
            message = "Вы сразу перейдёте к записи в журнале канцелярии, не дочитав вступление про " +
                    "город и первые странности. Вернуться и дочитать пролог потом будет нельзя.",
            confirmLabel = "Пропустить",
            cancelLabel = "Остаться"
        ),
        effects = listOf(Effect.Transition("prologue_16", "prologue"))
    )

    // ---------- Пролог: 16 страниц ----------

    private val prologue01 = Scene(
        id = "prologue_01",
        locationId = "prologue",
        text = "Риверстоун стоял там, где большая река замедляла свой бег перед тем, как уйти на юг.\n\n" +
                "С севера к городу вели торговые дороги, с запада вел старый тракт. С востока начинались " +
                "земли, где лес постепенно становился гуще, а поселения реже. Река же была самой надёжной " +
                "дорогой.\n\n" +
                "Город рос вокруг неё много столетий.\n\n" +
                "Сначала появилась крепость на высоком каменном берегу, потом пристань, за ней выросли " +
                "рынки, храмы, постоялые дворы и множество домов зажиточных граждан.",
        partTitle = PartTitle(
            chapterImage = "chapter_title.png",
            partImage = "part_title_prologue.png"
        ),
        actions = listOf(
            Action(id = "p01_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_02"))),
            skipPrologueAction("p01")
        )
    )

    private val prologue02 = Scene(
        id = "prologue_02",
        locationId = "prologue",
        text = "В старых районах улицы были настолько старыми и тесными, что два человека с трудом могли " +
                "разойтись, не задев друг друга плечами.\n\n" +
                "Это был не самый большой город королевства, но достаточно большой, чтобы в нём можно было " +
                "потеряться.",
        actions = listOf(
            Action(id = "p02_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_03"))),
            Action(id = "p02_back", text = "Назад", effects = listOf(Effect.Transition("prologue_01"))),
            skipPrologueAction("p02")
        )
    )

    private val prologue03 = Scene(
        id = "prologue_03",
        locationId = "prologue",
        text = "Днём город казался обычным.\n\n" +
                "На Рыночной площади торговцы перекрикивали друг друга, зазывалы рекламировали дешёвые " +
                "товары, кузнецы ругались с поставщиками угля, а грузчики у речных ворот таскали ящики до " +
                "тех пор, пока руки не начинали дрожать от усталости.\n\n" +
                "На главных улицах постоянно можно было увидеть людей в темно-серых плащах городской стражи.\n\n" +
                "Они стояли у ворот, патрулировали площади, разбирали драки в тавернах, ловили карманников " +
                "и следили за порядком в порту.",
        actions = listOf(
            Action(id = "p03_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_04"))),
            Action(id = "p03_back", text = "Назад", effects = listOf(Effect.Transition("prologue_02"))),
            skipPrologueAction("p03")
        )
    )

    private val prologue04 = Scene(
        id = "prologue_04",
        locationId = "prologue",
        text = "Когда лавки закрывались, а последние посетители покидали трактиры, город не засыпал, а " +
                "просто менялся.\n\n" +
                "На улицах зажигались фонари, из таверн доносились песни, на пристани продолжали работать " +
                "грузчики. Где-то хлопали ставни, где-то громко ругались, где-то было слышно разговоры " +
                "вполголоса, а по мостовой размеренно проходили патрули.",
        actions = listOf(
            Action(id = "p04_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_05"))),
            Action(id = "p04_back", text = "Назад", effects = listOf(Effect.Transition("prologue_03"))),
            skipPrologueAction("p04")
        )
    )

    private val prologue05 = Scene(
        id = "prologue_05",
        locationId = "prologue",
        text = "Первые странности начались настолько незаметно, что никто не смог потом назвать точный день.",
        actions = listOf(
            Action(id = "p05_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_06"))),
            Action(id = "p05_back", text = "Назад", effects = listOf(Effect.Transition("prologue_04"))),
            skipPrologueAction("p05")
        )
    )

    private val prologue06 = Scene(
        id = "prologue_06",
        locationId = "prologue",
        text = "Один из ночных патрулей сообщил, что несколько раз слышал шаги за собой на пустой улице. " +
                "Когда стражники оборачивались, там никого не было.",
        actions = listOf(
            Action(id = "p06_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_07"))),
            Action(id = "p06_back", text = "Назад", effects = listOf(Effect.Transition("prologue_05"))),
            skipPrologueAction("p06")
        )
    )

    private val prologue07 = Scene(
        id = "prologue_07",
        locationId = "prologue",
        text = "В другой части города женщина пожаловалась, что каждую ночь кто-то трижды стучит в её " +
                "дверь. Никто не стоял за порогом. Потом это произошло у её соседки. А затем ещё у одного дома.",
        actions = listOf(
            Action(id = "p07_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_08"))),
            Action(id = "p07_back", text = "Назад", effects = listOf(Effect.Transition("prologue_06"))),
            skipPrologueAction("p07")
        )
    )

    private val prologue08 = Scene(
        id = "prologue_08",
        locationId = "prologue",
        text = "В порту один лодочник отказался выходить на реку после заката.\n\n" +
                "Он утверждал, что видел свет фонаря на воде. Фонарь двигался против течения.\n\n" +
                "Лодочник получил насмешки от товарищей и неделю спустя снова вышел на реку.\n\n" +
                "Больше его никто не видел. Его лодку нашли утром, она была привязана к пристани. Пустая.",
        actions = listOf(
            Action(id = "p08_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_09"))),
            Action(id = "p08_back", text = "Назад", effects = listOf(Effect.Transition("prologue_07"))),
            skipPrologueAction("p08")
        )
    )

    private val prologue09 = Scene(
        id = "prologue_09",
        locationId = "prologue",
        text = "Такие истории появлялись всё чаще. Одни были совершенно безобидными, другие странными. " +
                "Некоторые заканчивались обычным объяснением, некоторые — нет.",
        actions = listOf(
            Action(id = "p09_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_10"))),
            Action(id = "p09_back", text = "Назад", effects = listOf(Effect.Transition("prologue_08"))),
            skipPrologueAction("p09")
        )
    )

    private val prologue10 = Scene(
        id = "prologue_10",
        locationId = "prologue",
        text = "Горожане начали шептаться. Сначала тихо, потом громче. На рынке обсуждали призраков, в " +
                "тавернах рассказывали о контрабандистах. Старики вспоминали старые легенды, а священники " +
                "говорили о дурных знамениях.\n\n" +
                "А те, кто не верил ни в призраков, ни в знамения, просто советовали не ходить ночью в " +
                "Нижний город.",
        actions = listOf(
            Action(id = "p10_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_11"))),
            Action(id = "p10_back", text = "Назад", effects = listOf(Effect.Transition("prologue_09"))),
            skipPrologueAction("p10")
        )
    )

    private val prologue11 = Scene(
        id = "prologue_11",
        locationId = "prologue",
        text = "Стража всё это время продолжала делать свою работу. Патрули стали чаще, у ворот " +
                "проверяли документы, на пристани появились дополнительные посты, но странные события " +
                "происходили вновь и вновь.\n\n" +
                "Так продолжалось до тех пор, пока люди не начали жаловаться бургомистру.",
        actions = listOf(
            Action(id = "p11_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_12"))),
            Action(id = "p11_back", text = "Назад", effects = listOf(Effect.Transition("prologue_10"))),
            skipPrologueAction("p11")
        )
    )

    private val prologue12 = Scene(
        id = "prologue_12",
        locationId = "prologue",
        text = "И чем больше стража пыталась убедить людей, что причин для паники нет, тем сильнее " +
                "становилась тревога. Однажды вечером на Рыночной площади кто-то сказал:\n\n" +
                "— «Если стража ничего не может найти, значит, она просто не хочет искать».\n\n" +
                "Эти слова услышали десятки людей, а к утру их повторял уже весь город.",
        actions = listOf(
            Action(id = "p12_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_13"))),
            Action(id = "p12_back", text = "Назад", effects = listOf(Effect.Transition("prologue_11"))),
            skipPrologueAction("p12")
        )
    )

    private val prologue13 = Scene(
        id = "prologue_13",
        locationId = "prologue",
        text = "Через несколько дней на доске объявлений у городской ратуши появилось объявление.\n\n" +
                "Оно было написано аккуратным официальным почерком и скреплено печатью мэрии.\n\n" +
                "ГОРОДСКАЯ МЭРИЯ РИВЕРСТОУНА\n\n" +
                "В связи с участившимися случаями нарушений общественного порядка, исчезновений имущества " +
                "и иными обстоятельствами, вызывающими беспокойство среди жителей города, объявляется поиск " +
                "лиц, способных оказать содействие городской администрации корпусу городской стражи в " +
                "расследовании указанных происшествий.\n\n" +
                "Лица, располагающие необходимыми навыками и готовые содействовать установлению причин " +
                "происходящего, могут обратиться в городскую ратушу.\n\n" +
                "Вознаграждение за оказанную помощь администрация гарантирует.\n\n" +
                "Дополнительные сведения — в канцелярии мэрии.",
        actions = listOf(
            Action(id = "p13_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_14"))),
            Action(id = "p13_back", text = "Назад", effects = listOf(Effect.Transition("prologue_12"))),
            skipPrologueAction("p13")
        )
    )

    private val prologue14 = Scene(
        id = "prologue_14",
        locationId = "prologue",
        text = "Объявление читали по-разному: одни пожимали плечами, другие усмехались. Кто-то говорил, " +
                "что городская власть наконец решила признать проблему, кто-то, что бургомистр просто " +
                "пытается найти виноватого.\n\n" +
                "А некоторые сразу начали подсчитывать, сколько можно заработать.\n\n" +
                "В Риверстоуне всегда находилась работа для того, кто не боялся испачкать руки.",
        actions = listOf(
            Action(id = "p14_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_15"))),
            Action(id = "p14_back", text = "Назад", effects = listOf(Effect.Transition("prologue_13"))),
            skipPrologueAction("p14")
        )
    )

    private val prologue15 = Scene(
        id = "prologue_15",
        locationId = "prologue",
        text = "Вы, как и многие другие желающие обогатиться за счёт мэрии города, отправились сразу в " +
                "канцелярию.\n\n" +
                "В приёмной было тесно и шумно. Искатели удачи толпились у барьера, шуршали бумагами, " +
                "спорили о том, кому что причитается, и косились друг на друга, прикидывая, чьи шансы " +
                "выше. Вы терпеливо дождались своей очереди и подошли к столу служащего.\n\n" +
                "Он выслушал вас без лишних вопросов, лишь изредка кивая. Было видно, что подобных вам " +
                "он видел сегодня десятки. Когда вы закончили, он вздохнул, придвинул к себе тяжёлый " +
                "журнал и обмакнул перо в чернильницу.",
        actions = listOf(
            Action(id = "p15_continue", text = "Продолжить", effects = listOf(Effect.Transition("prologue_16"))),
            Action(id = "p15_back", text = "Назад", effects = listOf(Effect.Transition("prologue_14"))),
            skipPrologueAction("p15")
        )
    )

    private val prologue16 = Scene(
        id = "prologue_16",
        locationId = "prologue",
        text = "Канцелярия Риверстоуна было одно из немногих мест в городе, где странные происшествия " +
                "последних недель превратились в обычную бумажную работу.\n\n" +
                "Служащий говорил сухо и без выражения, словно зачитывал давно заученный текст. Мэрия " +
                "назначила награду тем, кто сумеет докопаться до правды. Каждому желающему выдаётся " +
                "конверт с печатью канцелярии, а в нём находится короткое описание одного из происшествий " +
                "и адрес, с которого следует начинать поиски. Размер вознаграждения зависит только от " +
                "результата, а любые дополнительные расходы придётся согласовывать отдельно.\n\n" +
                "– Городская стража уже пыталась разобраться в случившемся, – добавил он, – но ясного " +
                "результата не добилась. Дальше дело переходит в ваши руки.\n\n" +
                "Он придвинул журнал ближе и занёс перо над чистой строкой.\n\n" +
                "– Для начала формальности. Как вас записать? И чем вы располагаете: что умеете, на что " +
                "рассчитываете?",
        actions = listOf(
            Action(
                id = "p16_create_character",
                text = "Заполнить журнал канцелярии",
                requiresCharacterCreation = true,
                effects = listOf(Effect.Transition("case_intro", "city_hall"))
            ),
            Action(id = "p16_back", text = "Назад", effects = listOf(Effect.Transition("prologue_15")))
        )
    )

    val startingScene = prologue01

    // ---------- После создания персонажа ----------

    private const val CASE_INTRO_COMMON =
        "Служащий занёс ваше имя в журнал, вручил конверт и, пряча раздражение от вашего визита под " +
                "лицемерной улыбкой, произнёс: «Надеюсь, вам улыбнётся удача в поиске ответов». Ведомый желанием " +
                "побыстрее решить плевое дело и заработать, вы уверенным шагом вышли из канцелярии.\n\n" +
                "Выйдя из канцелярии, вы остановились на ступенях ратуши и вскрыли конверт, запечатанный гербовой " +
                "печатью Риверстоуна. Внутри лежали документы, отчеты стражи и адрес, пожалуй, это был отличный " +
                "способ заработать."

    private val caseIntro = Scene(
        id = "case_intro",
        locationId = "city_hall",
        text = CASE_INTRO_COMMON,
        textVariants = listOf(
            TextVariant(
                Condition.HasArchetype(Archetype.VETERAN),
                CASE_INTRO_COMMON + "\n\n" +
                        "За годы службы ветерану войны приходилось сталкиваться с самыми разными " +
                        "неприятностями, и городские странности едва ли могли удивить после всего " +
                        "пережитого. Но теперь война осталась позади, а деньги всё ещё были нужны. Новое " +
                        "дело могло стать хорошим началом спокойной жизни — если, конечно, не принесёт " +
                        "новых неприятностей."
            ),
            TextVariant(
                Condition.HasArchetype(Archetype.DETECTIVE),
                CASE_INTRO_COMMON + "\n\n" +
                        "Работа обещала не только деньги. Неизвестное дело, о котором городская стража не " +
                        "смогла дать ясного ответа, само по себе представляло интерес. Возможно, в этих " +
                        "нескольких строках скрывалась загадка, достойная внимания. Оставалось проверить, " +
                        "насколько хорошо составлено описание и что удалось упустить тем, кто занимался " +
                        "этим раньше."
            ),
            TextVariant(
                Condition.HasArchetype(Archetype.HISTORIAN),
                CASE_INTRO_COMMON + "\n\n" +
                        "Деньги были приятным дополнением, но куда больше заинтересовало само дело. " +
                        "Риверстоун был старым городом, и за странными происшествиями нередко скрывались " +
                        "события далёкого прошлого. Возможно, это всего лишь совпадение, а, возможно, " +
                        "происходящее каким-то образом связано с тем, что уже однажды случалось в городе. " +
                        "В любом случае стоило разобраться — особенно если по пути удастся узнать что-нибудь, " +
                        "чего ещё нет ни в одной летописи."
            ),
            TextVariant(
                Condition.HasArchetype(Archetype.INNKEEPER),
                CASE_INTRO_COMMON + "\n\n" +
                        "После пожара деньги были нужны как никогда. Трактир сгорел почти без " +
                        "предупреждения, а причины возгорания так и остались неясными. Слишком много " +
                        "странного произошло той ночью, чтобы просто списать всё на случайность. Если " +
                        "нынешнее дело связано с другими событиями в городе, возможно, оно поможет понять, " +
                        "что на самом деле произошло с трактиром. А заодно даст возможность немного " +
                        "заработать, пока приходится начинать всё заново."
            ),
            TextVariant(
                Condition.HasArchetype(Archetype.KNIGHT),
                CASE_INTRO_COMMON + "\n\n" +
                        "Дело выглядело достаточно обычным, чтобы не придавать ему особого значения, тем " +
                        "более для рыцаря, но город обратился за помощью не просто так. Если стража не " +
                        "смогла разобраться, значит, причина могла оказаться серьёзнее, чем казалось на " +
                        "первый взгляд. Вознаграждение было приятным дополнением, но прежде всего это была " +
                        "возможность исполнить долг перед орденом — нести справедливость и добродетель."
            ),
            TextVariant(
                Condition.HasArchetype(Archetype.PHYSICIAN),
                CASE_INTRO_COMMON + "\n\n" +
                        "Деньги были не особо нужны, это дело заинтересовало по другой причине. За " +
                        "последнее время слишком много странностей можно было связать с болезнями, " +
                        "недомоганиями и внезапными смертями. Возникло подозрение, что в городе может " +
                        "распространяться эпидемия, о которой кто-то предпочитает молчать. Если это " +
                        "действительно так, промедление может стоить людям жизни. Нужно было выяснить " +
                        "правду — независимо от того, кому она окажется неудобна."
            ),
            TextVariant(
                Condition.HasArchetype(Archetype.ADVENTURER),
                CASE_INTRO_COMMON + "\n\n" +
                        "Наконец-то что-то похожее на настоящее приключение — и, что особенно приятно, за " +
                        "деньги. Не сокровища из старых руин и не спасение мира, но начало вполне " +
                        "подходящее. В любом случае это лучше, чем бесцельно сидеть без дела. Адрес был, " +
                        "поручение было, награда обещана, значит, пора отправляться."
            )
        ),
        actions = listOf(
            Action(
                id = "start_case_1",
                text = "Дело первое: Человек, которого не было",
                effects = listOf(
                    Effect.SetResource("gold", 10, "Золото", min = 0),
                    Effect.Transition(Chapter1.startingScene.id, Chapter1.startingScene.locationId)
                )
            )
        )
    )

    val scenes: List<Scene> = listOf(
        prologue01, prologue02, prologue03, prologue04, prologue05,
        prologue06, prologue07, prologue08, prologue09, prologue10,
        prologue11, prologue12, prologue13, prologue14, prologue15, prologue16,
        caseIntro
    ) + Chapter1.scenes

    val itemExamineActions: Map<String, Action> = Chapter1.itemExamineActions
}