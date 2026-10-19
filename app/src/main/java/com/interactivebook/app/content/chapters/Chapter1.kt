package com.interactivebook.app.content.chapters

import com.interactivebook.app.core.checks.Check
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.conditions.Condition
import com.interactivebook.app.core.effects.Effect
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.core.model.InformationType
import com.interactivebook.app.core.model.PartTitle
import com.interactivebook.app.core.model.Scene
import com.interactivebook.app.core.model.SkillType
import com.interactivebook.app.core.model.StatType

object Chapter1 {

    private val alleyEntrance = Scene(
        id = "c1_alley_entrance",
        locationId = "lower_city_alley",
        partTitle = PartTitle(
            chapterImage = "chapter_title.png",
            partImage = "part_title_case1.png"
        ),
        text = "Переулок Нижнего города встречает вас запахом сырого камня, помоев и вчерашнего " +
                "дождя. Между покосившимися домами едва пробивается серый дневной свет. На стенах " +
                "темнеют потёки влаги, а из щелей между булыжниками пробивается грязная трава.\n\n" +
                "У входа в переулок стоит стражник.",
        actions = listOf(
            Action(
                id = "approach_guard_first",
                text = "Подойти к стражнику",
                condition = Condition.HasFlag("seal_shown", false),
                effects = listOf(Effect.Transition("c1_guard_dialogue"))
            ),
            Action(
                id = "approach_guard_hub",
                text = "Подойти к стражнику",
                condition = Condition.And(listOf(Condition.HasFlag("seal_shown", true), Condition.HasFlag("guard_annoyed", false))),
                effects = listOf(Effect.Transition("c1_guard_recognized"))
            ),
            Action(
                id = "approach_guard_hub_annoyed",
                text = "Подойти к стражнику",
                condition = Condition.And(listOf(Condition.HasFlag("seal_shown", true), Condition.HasFlag("guard_annoyed", true))),
                effects = listOf(Effect.Transition("c1_guard_recognized_annoyed"))
            ),
            Action(
                id = "examine_crime_scene_entry",
                text = "Осмотреть место преступления",
                condition = Condition.And(listOf(Condition.HasFlag("evidence_asked", true), Condition.HasInformation("finger"))),
                effects = listOf(Effect.Transition("c1_crime_scene"))
            ),
            Action(
                id = "go_to_house_385",
                text = "Отправиться к дому убитого",
                condition = Condition.HasInformation("house_385"),
                effects = listOf(Effect.Transition("c1_house_385", "main_street"))
            )
        )
    )

    private val guardDialogue = Scene(
        id = "c1_guard_dialogue",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "Стражник заметил вас не сразу. Он стоял, прислонившись к стене, и лениво ковырял " +
                "носком сапога грязь между камнями. Заметив приближение, недовольно фыркнул и шагнул " +
                "навстречу, преграждая дорогу.\n\n" +
                "— Эй! Здесь не на что глазеть, — бросил он. — Проваливай.\n\n" +
                "В его голосе не было ни тени обычной стражи порядка. Скорее так разговаривал человек, " +
                "привыкший решать вопросы кулаками.",
        actions = listOf(
            Action(
                id = "show_seal",
                text = "Показать печать Риверстоуна на конверте",
                effects = listOf(Effect.SetFlag("seal_shown", true), Effect.Transition("c1_guard_recognized"))
            )
        )
    )

    private val guardRecognized = Scene(
        id = "c1_guard_recognized",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "Когда вы показали печать Риверстоуна на поручении, полученном в канцелярии, " +
                "выражение его лица немного изменилось. Улыбки не появилось, но прежняя открытая " +
                "враждебность сменилась тяжёлым, оценивающим взглядом. Он несколько мгновений изучал " +
                "печать, будто решал, сколько неприятностей она может ему доставить.\n\n" +
                "— Младший сержант Руперт, стража Риверстоуна, — нехотя представился он. — Так это " +
                "тебя из канцелярии прислали? Черти бы их побрали.\n\n" +
                "Он протянул руку, но не за бумагой — скорее требуя показать её ближе.\n\n" +
                "— Можешь осмотреться. Только учти: вздумаешь вынести отсюда что-нибудь ценное — " +
                "выбью из тебя душу и не посмотрю на твои важные бумаги.\n\n" +
                "Руперт ухмыльнулся.\n\n" +
                "— Я бы на твоём месте здесь долго не задерживался. Место недоброе.",
        actions = listOf(
            Action(
                id = "ask_incident",
                text = "Расспросить о случившемся",
                effects = listOf(Effect.Transition("c1_guard_incident"))
            ),
            Action(
                id = "ask_documents",
                text = "Расспросить о документах",
                effects = listOf(
                    Effect.AddInformation("victim_name", "Имя убитого", "Эдвард Гремм.", "Дело", InformationType.NOTE),
                    Effect.AddInformation(
                        "victim_details", "Заметки о найденном",
                        "Судя по бумагам, у покойного был дом в городе, и патруль уже проверял адрес.",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.AddItem("victim_documents", "Документы убитого", "Свёрток бумаг, писем и записей покойного.", 1),
                    Effect.AddInformation(
                        "docs_should_have_address", "В документах должен быть адрес",
                        "Среди бумаг Гремма должен быть указан адрес его дома.", "Дело", InformationType.GOAL
                    ),
                    Effect.Transition("c1_guard_documents")
                )
            ),
            Action(
                id = "end_talk",
                text = "Закончить разговор",
                isNavigation = true,
                effects = listOf(Effect.Transition("c1_alley_entrance"))
            )
        )
    )

    private val guardRecognizedAnnoyed = Scene(
        id = "c1_guard_recognized_annoyed",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "Стражник скрестил руки на груди и посмотрел на вас с откровенным раздражением.\n\n" +
                "Казалось, присутствие человека с поручением из канцелярии было для него не приказом, " +
                "а досадной помехой.",
        actions = listOf(
            Action(
                id = "ask_incident_annoyed",
                text = "Расспросить о случившемся",
                effects = listOf(Effect.Transition("c1_guard_incident"))
            ),
            Action(
                id = "ask_evidence",
                text = "Расспросить об уликах",
                condition = Condition.HasFlag("ivy_needs_retry", false),
                effects = listOf(Effect.SetFlag("evidence_asked", true), Effect.Transition("c1_guard_evidence"))
            ),
            Action(
                id = "ask_evidence_retry",
                text = "Расспросить об уликах",
                condition = Condition.HasFlag("ivy_needs_retry", true),
                effects = listOf(
                    Effect.SetFlag("ivy_needs_retry", false),
                    Effect.ChangeResource("ivy_retry_count", 1, "Попытки (плющ)"),
                    Effect.SetFlag("evidence_asked", true),
                    Effect.Transition("c1_guard_evidence")
                )
            ),
            Action(
                id = "ask_strangeness",
                text = "Расспросить о странностях",
                effects = listOf(Effect.Transition("c1_guard_strangeness"))
            ),
            Action(
                id = "examine_guard",
                text = "Рассмотреть стражника",
                condition = Condition.HasFlag("guard_observed", false),
                check = Check(StatType.INTELLECT, SkillType.OBSERVATION, 16),
                consequences = mapOf(
                    CheckOutcome.CRITICAL_SUCCESS to "c1_examine_guard_crit_success",
                    CheckOutcome.SUCCESS to "c1_examine_guard_success",
                    CheckOutcome.FAILURE to "c1_examine_guard_failure",
                    CheckOutcome.CRITICAL_FAILURE to "c1_examine_guard_crit_failure"
                )
            ),
            Action(
                id = "end_talk_annoyed",
                text = "Закончить разговор",
                isNavigation = true,
                effects = listOf(Effect.Transition("c1_alley_entrance"))
            )
        )
    )

    private val examineGuardCritSuccessScene = Scene(
        id = "c1_examine_guard_crit_success",
        locationId = "lower_city_alley",
        consequenceTitle = "РАССМОТРЕТЬ СТРАЖНИКА: КРИТИЧЕСКИЙ УСПЕХ",
        text = "Вы замечаете, что под сапогом Руперта лежит монета. Должно быть, одна из тех, " +
                "что были разбросаны после убийства.\n\n" +
                "Он её не замечает — взгляд стражника прикован к тёмному концу переулка.\n\n" +
                "Вы быстро поднимаете монету.\n\n" +
                "Руперт ничего не замечает.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.SetFlag("guard_observed", true),
                    Effect.AddInformation(
                        "guard_watching_corners", "Стражник что-то высматривает",
                        "Он часто оглядывается, внимательно смотрит в тёмные углы переулка и " +
                                "прислушивается к каждому шороху, словно опасается увидеть там нечто, " +
                                "чего видеть не хочет.", "Стражник", InformationType.NOTE
                    ),
                    Effect.ChangeResource("gold", 1, "Золото", min = 0),
                    Effect.Transition("c1_guard_rats")
                )
            )
        )
    )

    private val examineGuardSuccessScene = Scene(
        id = "c1_examine_guard_success",
        locationId = "lower_city_alley",
        consequenceTitle = "РАССМОТРЕТЬ СТРАЖНИКА: УСПЕХ",
        text = "Вы подмечаете нервозность Руперта, и для стражника это выглядит странно. Он часто " +
                "оглядывается, внимательно смотрит в тёмные углы переулка, прислушивается к каждому " +
                "шороху, словно опасается увидеть там нечто, чего видеть не хочет.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.SetFlag("guard_observed", true),
                    Effect.AddInformation(
                        "guard_watching_corners", "Стражник что-то высматривает",
                        "Он часто оглядывается, внимательно смотрит в тёмные углы переулка и " +
                                "прислушивается к каждому шороху, словно опасается увидеть там нечто, " +
                                "чего видеть не хочет.", "Стражник", InformationType.NOTE
                    ),
                    Effect.Transition("c1_guard_rats")
                )
            )
        )
    )

    private val examineGuardFailureScene = Scene(
        id = "c1_examine_guard_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "РАССМОТРЕТЬ СТРАЖНИКА: ПРОВАЛ",
        text = "Вы ничего особенного не замечаете, что отличало бы этого стражника от других. " +
                "Разве что он был немного более щетинист, чем принято у городской стражи, да доспехи " +
                "его выглядели так, словно он давно забыл, что их полагается чистить.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_guard_recognized_annoyed"))
            )
        )
    )

    private val examineGuardCritFailureScene = Scene(
        id = "c1_examine_guard_crit_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "РАССМОТРЕТЬ СТРАЖНИКА: КРИТИЧЕСКИЙ ПРОВАЛ",
        speakerName = "Стражник Руперт",
        text = "— Чего пялишься? — резко бросил Руперт. — Задумал что-то?\n\n" +
                "Он шагнул ближе.\n\n" +
                "— Ты мне не нравишься. Пошёл вон.\n\n" +
                "Не дожидаясь ответа, стражник толкнул вас плечом, выгоняя из переулка.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_guard_angry"))
            )
        )
    )

    private val guardIncident = Scene(
        id = "c1_guard_incident",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "— Вчера утром нашли тело, — начал Руперт, лениво осматривая вас с головы до ног. " +
                "— Мужик в дорогом наряде. Лежал вот здесь, у стены. Похоже, ночью кто-то хорошенько " +
                "с ним поработал.\n\n" +
                "Он кивнул на мостовую.\n\n" +
                "— При нём осталось всё: дорогая одежда, сапоги, документы. А вот с золотом вышла " +
                "целая история.\n\n" +
                "Руперт указал в глубину переулка.\n\n" +
                "— Монеты были разбросаны по всей улице. Пришлось собирать одну за другой. Часть " +
                "сдали в казну. Часть местный сброд успел растащить. Потом нашли монеты у них в " +
                "карманах, под рубахами, даже в сапогах. Пришлось отнимать.\n\n" +
                "Он усмехнулся краем рта.\n\n" +
                "— Видать, душегуба золото не интересовало.\n\n" +
                "Руперт окинул переулок взглядом.\n\n" +
                "— А одежду почти никто не тронул. Вот что странно. Видать, эти нищие даже не поняли, " +
                "сколько стоит хорошая ткань. Странное убийство... Кто-то явно не собирался ничего " +
                "забирать.\n\n" +
                "Он пожал плечами.\n\n" +
                "— Тогда зачем убивать? И зачем тащить сюда целое состояние, чтобы потом раскидать " +
                "его по этой помойке?",
        actions = listOf(
            Action(
                id = "leave_incident",
                text = "Спросить о чём-то другом",
                isNavigation = true,
                condition = Condition.HasFlag("guard_annoyed", false),
                effects = listOf(
                    Effect.AddInformation(
                        "scattered_coins", "Разбросанные монеты",
                        "По всему месту преступления были разбросаны золотые монеты, откуда они " +
                                "там взялись — неизвестно. Если их принёс убитый, то почему убийца их " +
                                "не взял? Если их подбросили, то какой в этом смысл и есть ли он?",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.Transition("c1_guard_recognized")
                )
            ),
            Action(
                id = "leave_incident_annoyed",
                text = "Спросить о чём-то другом",
                isNavigation = true,
                condition = Condition.HasFlag("guard_annoyed", true),
                effects = listOf(
                    Effect.AddInformation(
                        "scattered_coins", "Разбросанные монеты",
                        "По всему месту преступления были разбросаны золотые монеты, откуда они " +
                                "там взялись — неизвестно. Если их принёс убитый, то почему убийца их " +
                                "не взял? Если их подбросили, то какой в этом смысл и есть ли он?",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.Transition("c1_guard_recognized_annoyed")
                )
            )
        )
    )

    private val guardDocuments = Scene(
        id = "c1_guard_documents",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "Стражник достал из-за пояса небольшой свёрток, перехватил его грязными пальцами " +
                "и лениво покрутил в руке.\n\n" +
                "— Ещё у него нашли бумаги. Письма, записи, какие-то документы. Часть мы уже " +
                "просмотрели.\n\n" +
                "Он постучал пальцем по свёртку.\n\n" +
                "— Имя — Эдвард Гремм. Судя по бумагам, у покойного был дом в городе.\n\n" +
                "— Адрес тоже есть. Патруль уже сходил туда, проверил.\n\n" +
                "Руперт протянул свёрток вам.\n\n" +
                "— Вот. Всё, что нашли при нём и что не успели забрать в канцелярию.\n\n" +
                "Он усмехнулся.\n\n" +
                "— Разбирайся, если желание есть.",
        actions = listOf(
            Action(
                id = "ask_address",
                text = "Спросить про адрес",
                effects = listOf(Effect.Transition("c1_guard_snide"))
            ),
            Action(
                id = "leave_documents",
                text = "Спросить о чём-то другом",
                isNavigation = true,
                effects = listOf(Effect.Transition("c1_guard_recognized"))
            )
        )
    )

    private val guardSnide = Scene(
        id = "c1_guard_snide",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "— Неужто горе-сыщик не способен найти адрес в паре бумаг?\n\n" +
                "Руперт усмехнулся, обнажив неровные зубы.\n\n" +
                "— Подумай. Пораскинь мозгами. Может, сам догадаешься.\n\n" +
                "Медленно, по словам, как ребёнком, произнёс Руперт и смерил вас насмешливым взглядом.\n\n" +
                "— Или тебе обязательно всё разжевать и в рот положить? Раз уж из канцелярии прислали " +
                "такого умника, значит, там за тебя ручаются.\n\n" +
                "Он хмыкнул и отвернулся, явно получая удовольствие от собственной грубости.",
        actions = listOf(
            Action(
                id = "guard_takes_job",
                text = "Показать свое безразличие на сказанное",
                effects = listOf(
                    Effect.SetFlag("guard_annoyed", true),
                    Effect.AddInformation("goal_find_address", "Необходимо найти адрес", "", "Дело", InformationType.GOAL),
                    Effect.Transition("c1_guard_recognized_annoyed")
                )
            )
        )
    )

    private val guardEvidence = Scene(
        id = "c1_guard_evidence",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "— Ничего особенного не нашли, — Руперт пожал плечами. — Следы борьбы, кровь, " +
                "разбросанные монеты. Остальное уже собрали и унесли.\n\n" +
                "Он усмехнулся и кивнул на переулок.\n\n" +
                "— Но если думаешь, что мои ребята что-то проглядели — милости просим. Поищи сам.\n\n" +
                "— Вдруг… — Руперт сделал паузу и расплылся в улыбке. — Именно тебе повезёт найти то, " +
                "чего не заметили мы.\n\n" +
                "Стражник отступил в сторону и с нарочитой вежливостью поклонился, пропуская вас.\n\n" +
                "— Только смотри под ноги. Здесь уже всякий успел побывать.",
        actions = listOf(
            Action(
                id = "leave_evidence",
                text = "Спросить о чём-то другом",
                isNavigation = true,
                effects = listOf(Effect.Transition("c1_guard_recognized_annoyed"))
            )
        )
    )

    private val guardStrangeness = Scene(
        id = "c1_guard_strangeness",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "— Свидетелей почти нет, — Руперт нахмурился. — Тех, кого нашли поблизости, уже " +
                "опросили. Никто ничего не видел и не слышал.\n\n" +
                "Он презрительно оглядел убогие дома.\n\n" +
                "— Да и кого тут спрашивать? Одни трущобы. Ночью люди двери запирают на засовы и " +
                "стараются лишний раз не высовываться.\n\n" +
                "Он посмотрел туда, где нашли тело.\n\n" +
                "— Вот только непонятно, как он вообще здесь оказался. Посреди такой дыры и в " +
                "парадном костюме. Чистая одежда, дорогая ткань, хорошие сапоги... Будто на приём к " +
                "министру собрался.\n\n" +
                "Руперт сплюнул на мостовую.\n\n" +
                "— Может, с кем-то встречался. Только с кем? А потом его убили и ничего не взяли. " +
                "Даже золото.\n\n" +
                "Он снова покосился на монеты.\n\n" +
                "— Странно. Очень странно.\n\n" +
                "Руперт понизил голос.\n\n" +
                "— А теперь самое интересное. Из архива канцелярии пришла весть: имя убитого — Эдвард " +
                "Гремм.\n\n" +
                "Он нервно сглотнул.\n\n" +
                "— И по городским записям выходит, что этого самого Эдварда Гремма похоронили семь " +
                "лет назад.\n\n" +
                "Руперт оглянулся на пустые окна.\n\n" +
                "— Архивариусы уверены — ошибки нет. Перед нами именно Эдвард Гремм. Чертовщина " +
                "какая-то.",
        actions = listOf(
            Action(
                id = "ask_more_strangeness",
                text = "Может, ещё что-то?",
                check = Check(StatType.INTELLECT, SkillType.ELOQUENCE, 15),
                consequences = mapOf(
                    CheckOutcome.CRITICAL_SUCCESS to "c1_ask_more_crit_success",
                    CheckOutcome.SUCCESS to "c1_ask_more_success",
                    CheckOutcome.FAILURE to "c1_ask_more_failure",
                    CheckOutcome.CRITICAL_FAILURE to "c1_ask_more_crit_failure"
                )
            ),
            Action(
                id = "leave_strangeness",
                text = "Спросить о чём-то другом",
                isNavigation = true,
                effects = listOf(Effect.Transition("c1_guard_recognized_annoyed"))
            )
        )
    )

    private val askMoreCritFailureScene = Scene(
        id = "c1_ask_more_crit_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "МОЖЕТ, ЕЩЁ ЧТО-ТО: КРИТИЧЕСКИЙ ПРОВАЛ",
        speakerName = "Стражник Руперт",
        text = "— Может, тебе ещё рассказать, где я вчера нужду справлял?\n\n" +
                "Руперт оскалился.\n\n" +
                "— Отвали от меня, пока я тебя сам в темницу не упёк.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_guard_angry"))
            )
        )
    )

    private val askMoreFailureScene = Scene(
        id = "c1_ask_more_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "МОЖЕТ, ЕЩЁ ЧТО-ТО: ПРОВАЛ",
        speakerName = "Стражник Руперт",
        text = "Руперт ухмыльнулся.\n\n" +
                "— Можно и ещё кое-что рассказать.\n\n" +
                "Он протянул ладонь.\n\n" +
                "— Но не бесплатно.",
        actions = listOf(
            Action(
                id = "bribe_for_more",
                text = "Отдать две золотые",
                condition = Condition.ResourceCompare("gold", Condition.Comparison.AT_LEAST, 2),
                effects = listOf(
                    Effect.ChangeResource("gold", -2, "Золото", min = 0),
                    Effect.Transition("c1_guard_finger")
                )
            ),
            Action(
                id = "refuse_bribe",
                text = "Уйти ни с чем",
                isNavigation = true,
                effects = listOf(Effect.Transition("c1_guard_recognized_annoyed"))
            )
        )
    )

    private val askMoreSuccessScene = Scene(
        id = "c1_ask_more_success",
        locationId = "lower_city_alley",
        consequenceTitle = "МОЖЕТ, ЕЩЁ ЧТО-ТО: УСПЕХ",
        speakerName = "Стражник Руперт",
        text = "— Ну, есть ещё что-нибудь? — протянул Руперт, растянув губы в мерзкой улыбке.\n\n" +
                "В тусклом свете, едва пробивавшемся в грязный переулок, его лицо казалось ещё гаже " +
                "обычного. Улыбка обнажила пожелтевшие зубы и глубокие складки у рта. На миг вам " +
                "показалось, что даже мёртвые стены заброшенных домов выглядят приветливее этого " +
                "человека. Вы невольно подумали, что хмурый вид ему куда больше к лицу.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_guard_finger"))
            )
        )
    )

    private val askMoreCritSuccessScene = Scene(
        id = "c1_ask_more_crit_success",
        locationId = "lower_city_alley",
        consequenceTitle = "МОЖЕТ, ЕЩЁ ЧТО-ТО: КРИТИЧЕСКИЙ УСПЕХ",
        speakerName = "Стражник Руперт",
        text = "Стражник задумался, зажмурился и принялся что-то бубнить себе под нос, словно " +
                "читая молитву. Из его невнятного бормотания удавалось разобрать лишь обрывки " +
                "нелепых слов об огромных крысах, пожирающих людей заживо.\n\n" +
                "Пока Руперт с закрытыми глазами шептал свои странные слова, вы заметили ещё одну " +
                "монету возле его сапога. Дождавшись удобного мгновения, вы бесшумно наклонились, " +
                "ловко подняли её и спрятали в карман.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.AddInformation(
                        "giant_rats", "Огромные крысы",
                        "Стражник упоминает огромных крыс, которые снуют повсюду, и советует " +
                                "смотреть в оба — он думает, что откушенный палец может быть их рук делом.",
                        "Стражник", InformationType.NOTE
                    ),
                    Effect.ChangeResource("gold", 1, "Золото", min = 0),
                    Effect.Transition("c1_guard_finger")
                )
            )
        )
    )

    private val guardFinger = Scene(
        id = "c1_guard_finger",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "По архивным записям Эдварду Гремму на момент смерти было шестьдесят пять лет. " +
                "Найденному вчера мужику на вид было не больше тридцати пяти, но лицом он удивительно " +
                "напоминал Эдварда в молодости.\n\n" +
                "А ещё у мертвеца не хватало мизинца на правой руке. У Гремма тоже на руке не хватало " +
                "пальца с самого детства, хотя на какой именно руке, теперь уже никто точно не помнил.\n\n" +
                "Руперт заговорил тише. Старики шептались, что мертвец, вернувшийся на землю, может " +
                "помолодеть, но сохранить старые увечья. Семь лет прошло с похорон, а теперь появился " +
                "молодой мужчина с его именем, его вещами, похожий на него и без мизинца.\n\n" +
                "Руперт явно склонялся к мысли, что старик каким-то образом вернулся.\n\n" +
                "Правда, говоря об отсутствующем пальце, он почему-то замялся. На мгновение " +
                "задумался, будто знал другое объяснение, но предпочёл оставить его при себе.",
        actions = listOf(
            Action(
                id = "return_from_finger",
                text = "Вернуться к разговору",
                isNavigation = true,
                effects = listOf(
                    Effect.AddInformation("finger", "Про палец", "У найденного тела не хватает мизинца на правой руке — как и у Гремма.", "Дело", InformationType.NOTE),
                    Effect.AddInformation("age", "Про возраст", "Найденный мужчина выглядит лет на тридцать моложе, чем должен быть Гремм на момент смерти.", "Дело", InformationType.NOTE),
                    Effect.Transition("c1_guard_recognized_annoyed")
                )
            )
        )
    )

    private val guardAngry = Scene(
        id = "c1_guard_angry",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "— Чего опять заявился? — рявкнул Руперт.\n\n" +
                "Он смерил вас тяжёлым взглядом.\n\n" +
                "Похоже, подружиться с ним уже точно не получится.",
        actions = listOf(
            Action(
                id = "leave_angry",
                text = "Решил, что ты без меня будешь скучать",
                isNavigation = true,
                effects = listOf(Effect.SetFlag("guard_annoyed", true), Effect.Transition("c1_guard_recognized_annoyed"))
            )
        )
    )

    private val guardRats = Scene(
        id = "c1_guard_rats",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "Присмотревшись внимательнее, вы замечаете, что держится Руперт совсем не так " +
                "уверенно, как пытается показать.\n\n" +
                "Мужчина лет сорока, крепкий, широкоплечий, в потёртых доспехах городской стражи. На " +
                "лице густая щетина, под глазами залегли тёмные круги, а кожа на щеках покрыта " +
                "следами старых порезов.\n\n" +
                "Он то и дело оглядывается на конец переулка, прислушивается к каждому шороху и " +
                "машинально кладёт ладонь на рукоять меча.\n\n" +
                "На вопросы отвечает быстро. Иногда запинается. Когда разговор касается найденного " +
                "тела, старается перевести тему или отшутиться.\n\n" +
                "Похоже, Руперт чего-то опасается.\n\n" +
                "И, судя по всему, не разбойников.",
        actions = listOf(
            Action(
                id = "ask_nervous",
                text = "Ты чего такой нервный? Боишься чего? Тут нет никого кроме нас.",
                check = Check(StatType.INTELLECT, SkillType.ELOQUENCE, 17),
                consequences = mapOf(
                    CheckOutcome.CRITICAL_SUCCESS to "c1_nervous_crit_success",
                    CheckOutcome.SUCCESS to "c1_nervous_success",
                    CheckOutcome.FAILURE to "c1_nervous_failure",
                    CheckOutcome.CRITICAL_FAILURE to "c1_nervous_crit_failure"
                )
            )
        )
    )

    private val nervousCritFailureScene = Scene(
        id = "c1_nervous_crit_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "ТЫ ЧЕГО ТАКОЙ НЕРВНЫЙ: КРИТИЧЕСКИЙ ПРОВАЛ",
        speakerName = "Стражник Руперт",
        text = "Глаза стражника округляются от удивления и злости.\n\n" +
                "— Намекаешь, что я трус?\n\n" +
                "Руперт хватает вас за грудки и резко притягивает к себе.\n\n" +
                "— А ну проваливай, пока ходить можешь.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_guard_angry"))
            )
        )
    )

    private val nervousFailureScene = Scene(
        id = "c1_nervous_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "ТЫ ЧЕГО ТАКОЙ НЕРВНЫЙ: ПРОВАЛ",
        speakerName = "Стражник Руперт",
        text = "— Тебе показалось, — недовольно фыркает Руперт.\n\n" +
                "Он поправляет ремень на доспехах и делает вид, будто совершенно спокоен.\n\n" +
                "Но взгляд его снова скользит к тёмному углу переулка.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_guard_recognized_annoyed"))
            )
        )
    )

    private val nervousSuccessScene = Scene(
        id = "c1_nervous_success",
        locationId = "lower_city_alley",
        consequenceTitle = "ТЫ ЧЕГО ТАКОЙ НЕРВНЫЙ: УСПЕХ",
        speakerName = "Стражник Руперт",
        text = "— По мне видно? — удивлённо спрашивает Руперт.\n\n" +
                "На мгновение его привычная наглость исчезает.\n\n" +
                "— Я с детства крыс боюсь. Мне всё кажется, что они тут по углам шныряют. Я их " +
                "слышу. Даже когда не вижу.\n\n" +
                "Он нервно проводит рукой по лицу.\n\n" +
                "— У Гремма на правой руке мизинца не было. Это они, должно быть. Крысы. Их работа, " +
                "не иначе.\n\n" +
                "Руперт скривился.\n\n" +
                "— Проклятые твари. Ненавижу.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.AddInformation(
                        "rats", "Крысы",
                        "Руперт с детства боится крыс. Ему кажется, что они шныряют по углам " +
                                "переулка, даже когда он их не видит. Он думает, что мизинец Гремма " +
                                "могли отгрызть именно крысы.",
                        "Стражник", InformationType.NOTE, linkIfPresent = "guard_watching_corners"
                    ),
                    Effect.AddInformation(
                        "finger", "Про палец",
                        "У найденного тела не хватает мизинца на правой руке — как и у Гремма.",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.Transition("c1_guard_recognized_annoyed")
                )
            )
        )
    )

    private val nervousCritSuccessScene = Scene(
        id = "c1_nervous_crit_success",
        locationId = "lower_city_alley",
        consequenceTitle = "ТЫ ЧЕГО ТАКОЙ НЕРВНЫЙ: КРИТИЧЕСКИЙ УСПЕХ",
        speakerName = "Стражник Руперт",
        text = "— По мне видно? — удивляется Руперт.\n\n" +
                "На его лице на мгновение появляется неподдельный страх.\n\n" +
                "— Я с детства крыс боюсь. Мне всё кажется, что они тут шныряют. Снуют по углам, " +
                "под камнями... Я их слышу. У Гремма мизинца на правой руке не было. Это они, " +
                "должно быть. Их работа. Проклятые крысы.\n\n" +
                "Он резко меняется в лице.\n\n" +
                "— Я так боюсь здесь один стоять...\n\n" +
                "Руперт осекается, поняв, что сказал лишнее.\n\n" +
                "Несколько секунд молчит, затем натягивает на лицо привычную ухмылку.\n\n" +
                "— Послушай. Ты главное никому не разболтай, хорошо?\n\n" +
                "Он незаметно вкладывает вам в ладонь монету.\n\n" +
                "— Будем считать, что этого разговора не было.\n\n" +
                "Он похлопывает вас по плечу.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.AddInformation(
                        "rats", "Крысы",
                        "Руперт с детства боится крыс. Ему кажется, что они шныряют по углам " +
                                "переулка, даже когда он их не видит. Он думает, что мизинец Гремма " +
                                "могли отгрызть именно крысы.",
                        "Стражник", InformationType.NOTE, linkIfPresent = "guard_watching_corners"
                    ),
                    Effect.AddInformation(
                        "finger", "Про палец",
                        "У найденного тела не хватает мизинца на правой руке — как и у Гремма.",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.ChangeResource("gold", 1, "Золото", min = 0),
                    Effect.Transition("c1_guard_recognized_annoyed")
                )
            )
        )
    )

    private fun ivyExamineAction(exactRetries: Int?, atLeastRetries: Int?, dc: Int): Action {
        val conditions = mutableListOf<Condition>(
            Condition.HasFlag("ivy_resolved", false),
            Condition.HasFlag("ivy_needs_retry", false)
        )
        if (exactRetries != null) {
            conditions += Condition.ResourceCompare("ivy_retry_count", Condition.Comparison.AT_LEAST, exactRetries)
            conditions += Condition.ResourceCompare("ivy_retry_count", Condition.Comparison.AT_MOST, exactRetries)
        } else if (atLeastRetries != null) {
            conditions += Condition.ResourceCompare("ivy_retry_count", Condition.Comparison.AT_LEAST, atLeastRetries)
        }
        return Action(
            id = "examine_ivy_dc$dc",
            text = "Осмотреть плющ у стены",
            condition = Condition.And(conditions),
            check = Check(StatType.INTELLECT, SkillType.OBSERVATION, dc),
            consequences = mapOf(
                CheckOutcome.CRITICAL_SUCCESS to "c1_ivy_crit_success",
                CheckOutcome.SUCCESS to "c1_ivy_success",
                CheckOutcome.FAILURE to "c1_ivy_failure",
                CheckOutcome.CRITICAL_FAILURE to "c1_ivy_crit_failure"
            )
        )
    }

    private val ivyActions: List<Action> =
        (0..5).map { ivyExamineAction(exactRetries = it, atLeastRetries = null, dc = 14 + it) } +
                ivyExamineAction(exactRetries = null, atLeastRetries = 6, dc = 20)

    private val ivyDisabledPlaceholder = Action(
        id = "examine_ivy_disabled",
        text = "Осмотреть плющ у стены",
        condition = Condition.And(listOf(Condition.HasFlag("ivy_resolved", false), Condition.HasFlag("ivy_needs_retry", true))),
        disabledReason = "нужно подумать, что сделать ещё"
    )

    private val crimeScene = Scene(
        id = "c1_crime_scene",
        locationId = "lower_city_alley",
        text = "Переулок оказался узким и тихим. По обе стороны теснились старые дома с " +
                "потемневшими от времени стенами и окнами, забитыми гнилыми досками.\n\n" +
                "Тело нашли у глухой каменной стены, где мостовая почти вплотную подходила к " +
                "фундаменту. На камнях ещё темнели следы крови, а между булыжниками засохла грязь.\n\n" +
                "Почти всю стену рядом с местом убийства оплетал старый плющ. Толстые побеги " +
                "цеплялись за камень и уходили вверх, скрывая трещины и щели. В нескольких местах " +
                "зелень была примята и оборвана, словно кто-то недавно пробирался сквозь неё.\n\n" +
                "Теперь переулок снова выглядел обыденно.\n\n" +
                "Слишком обыденно для места, где ещё вчера лежал мертвец.",
        actions = listOf(
            Action(
                id = "examine_pavement",
                text = "Осмотреть мостовую",
                condition = Condition.HasFlag("pavement_resolved", false),
                check = Check(StatType.INTELLECT, SkillType.OBSERVATION, 16),
                consequences = mapOf(
                    CheckOutcome.CRITICAL_SUCCESS to "c1_pavement_crit_success",
                    CheckOutcome.SUCCESS to "c1_pavement_success",
                    CheckOutcome.FAILURE to "c1_pavement_failure",
                    CheckOutcome.CRITICAL_FAILURE to "c1_pavement_crit_failure"
                )
            )
        ) + ivyActions + ivyDisabledPlaceholder + Action(
            id = "finish_crime_scene",
            text = "Закончить осмотр",
            isNavigation = true,
            effects = listOf(Effect.Transition("c1_alley_entrance"))
        )
    )

    private val ivyCritFailureScene = Scene(
        id = "c1_ivy_crit_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "ОСМОТРЕТЬ ПЛЮЩ У СТЕНЫ: КРИТИЧЕСКИЙ ПРОВАЛ",
        text = "Вам кажется, что в зарослях плюща может быть что-то важное.\n\n" +
                "Вы осторожно раздвигаете побеги и проверяете стену. Вдруг из зелени выскакивает " +
                "огромная крыса и впивается зубами в палец.\n\n" +
                "От боли вы падаете на мостовую. Из кошелька вылетают золотые монеты и с грохотом " +
                "рассыпаются по переулку.\n\n" +
                "Руперт отшатывается от вас, будто вы заразны.\n\n" +
                "— В лазарет! Живо!",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.ChangeResource("gold", -5, "Золото", min = 0),
                    Effect.Transition("c1_guard_angry")
                )
            )
        )
    )

    private val ivyFailureScene = Scene(
        id = "c1_ivy_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "ОСМОТРЕТЬ ПЛЮЩ У СТЕНЫ: ПРОВАЛ",
        text = "Вам кажется, что в зарослях плюща может быть что-то важное.\n\n" +
                "Вы почти час проверяете каждый дюйм стены, раздвигаете спутанные побеги, " +
                "заглядываете в каждую щель.\n\n" +
                "Но безуспешно.\n\n" +
                "Только сырой камень, грязь и запах гнили.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.SetFlag("ivy_needs_retry", true),
                    Effect.Transition("c1_crime_scene")
                )
            )
        )
    )

    private val ivySuccessScene = Scene(
        id = "c1_ivy_success",
        locationId = "lower_city_alley",
        consequenceTitle = "ОСМОТРЕТЬ ПЛЮЩ У СТЕНЫ: УСПЕХ",
        text = "Вам кажется, что в зарослях плюща может быть что-то важное.\n\n" +
                "Аккуратными движениями вы раздвигаете спутанные побеги и наконец замечаете среди " +
                "камней металлический блеск.\n\n" +
                "Кольцо.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.SetFlag("ivy_resolved", true),
                    Effect.AddItem(
                        "signet_ring", "Кольцо-печатка с гербом",
                        "Металлическое кольцо с гербом, найденное среди побегов плюща.", 1
                    ),
                    Effect.AddInformation(
                        "ring_examinable_note", "Кольцо стоит рассмотреть",
                        "Кольцо можно рассмотреть внимательнее — возможно, оно подскажет что-то новое.",
                        "Предметы", InformationType.NOTE
                    ),
                    Effect.Transition("c1_crime_scene")
                )
            )
        )
    )

    private val ivyCritSuccessScene = Scene(
        id = "c1_ivy_crit_success",
        locationId = "lower_city_alley",
        consequenceTitle = "ОСМОТРЕТЬ ПЛЮЩ У СТЕНЫ: КРИТИЧЕСКИЙ УСПЕХ",
        text = "Вам кажется, что в зарослях плюща может быть что-то важное.\n\n" +
                "Аккуратными движениями вы проверяете стену и наконец находите кольцо.\n\n" +
                "Потянувшись за ним, вы замечаете рядом ещё две золотые монеты.\n\n" +
                "Вот это удача!",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.SetFlag("ivy_resolved", true),
                    Effect.AddItem(
                        "signet_ring", "Кольцо-печатка с гербом",
                        "Металлическое кольцо с гербом, найденное среди побегов плюща.", 1
                    ),
                    Effect.AddInformation(
                        "ring_examinable_note", "Кольцо стоит рассмотреть",
                        "Кольцо можно рассмотреть внимательнее — возможно, оно подскажет что-то новое.",
                        "Предметы", InformationType.NOTE
                    ),
                    Effect.ChangeResource("gold", 2, "Золото", min = 0),
                    Effect.Transition("c1_crime_scene")
                )
            )
        )
    )

    private val pavementCritFailureScene = Scene(
        id = "c1_pavement_crit_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "ОСМОТРЕТЬ МОСТОВУЮ: КРИТИЧЕСКИЙ ПРОВАЛ",
        speakerName = "Стражник Руперт",
        text = "Вы замечаете за камнем у мостовой тусклый блеск.\n\n" +
                "Монета.\n\n" +
                "Стоило вам только нагнуться, как Руперт вас заметил.\n\n" +
                "— Эй! Воровать вздумал?\n\n" +
                "Он протянул руку.\n\n" +
                "— Неси сюда монету. И чтоб глаза мои тебя больше не видели.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_guard_angry"))
            )
        )
    )

    private val pavementFailureScene = Scene(
        id = "c1_pavement_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "ОСМОТРЕТЬ МОСТОВУЮ: ПРОВАЛ",
        text = "Вы пристально всматриваетесь в серую мостовую. Она холодна, как и весь этот " +
                "город. Влажный камень отдаёт сыростью, а холод пробегает по спине.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_crime_scene"))
            )
        )
    )

    private val pavementSuccessScene = Scene(
        id = "c1_pavement_success",
        locationId = "lower_city_alley",
        consequenceTitle = "ОСМОТРЕТЬ МОСТОВУЮ: УСПЕХ",
        text = "Вы замечаете за камнем у мостовой тусклый блеск.\n\n" +
                "Монета.\n\n" +
                "Не оставлять же её здесь.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.SetFlag("pavement_resolved", true),
                    Effect.ChangeResource("gold", 1, "Золото", min = 0),
                    Effect.Transition("c1_crime_scene")
                )
            )
        )
    )

    private val pavementCritSuccessScene = Scene(
        id = "c1_pavement_crit_success",
        locationId = "lower_city_alley",
        consequenceTitle = "ОСМОТРЕТЬ МОСТОВУЮ: КРИТИЧЕСКИЙ УСПЕХ",
        speakerName = "Стражник Руперт",
        text = "Вы замечаете за камнем у мостовой блеск. Это монета. Стоило вам нагнуться, как " +
                "Руперт заметил находку.\n\n" +
                "— Ещё одну нашёл? Тут их много было.\n\n" +
                "Он оглядел переулок и сунул руки за пояс.\n\n" +
                "— Вот возьми ещё эти две и отнеси в канцелярию. Мне с поста уходить нельзя.\n\n" +
                "Руперт усмехнулся.\n\n" +
                "— А ты лицо доверенное. Через тебя и передам.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.SetFlag("pavement_resolved", true),
                    Effect.ChangeResource("gold", 3, "Золото", min = 0),
                    Effect.Transition("c1_crime_scene")
                )
            )
        )
    )

    private val guardRingReaction = Scene(
        id = "c1_guard_ring_reaction",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "Вы показали стражнику найденное кольцо.\n\n" +
                "Руперт взял его в ладонь, поднёс к тусклому свету, пробивавшемуся между тесными " +
                "домами Нижнего города, и долго рассматривал печатку.\n\n" +
                "Герб он узнал сразу. Это было заметно по тому, как на мгновение изменилось его лицо.\n\n" +
                "Руперт нахмурился, повертел кольцо между пальцами и провёл большим пальцем по " +
                "затёртому металлу.",
        actions = listOf(
            Action(
                id = "ask_about_ring",
                text = "Спросить о кольце-печатке",
                check = Check(StatType.INTELLECT, SkillType.ELOQUENCE, 16),
                consequences = mapOf(
                    CheckOutcome.CRITICAL_SUCCESS to "c1_ask_ring_crit_success",
                    CheckOutcome.SUCCESS to "c1_ask_ring_success",
                    CheckOutcome.FAILURE to "c1_ask_ring_failure",
                    CheckOutcome.CRITICAL_FAILURE to "c1_ask_ring_crit_failure"
                )
            )
        )
    )

    private val askRingCritFailureScene = Scene(
        id = "c1_ask_ring_crit_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "СПРОСИТЬ О КОЛЬЦЕ-ПЕЧАТКЕ: КРИТИЧЕСКИЙ ПРОВАЛ",
        speakerName = "Стражник Руперт",
        text = "Вы пытаетесь рассмотреть кольцо, поворачивая его в руках, и не замечаете, что за " +
                "спиной уже стоит Руперт.\n\n" +
                "— Ты где это нашёл?\n\n" +
                "Он резко вырывает кольцо из ваших пальцев.\n\n" +
                "— Хотел себе в карман сунуть?\n\n" +
                "Стражник презрительно усмехается.\n\n" +
                "— Я, думаешь, для чего здесь стою? Чтобы такие, прикрывающиеся благими делами, " +
                "мародёры не таскались по месту убийства?\n\n" +
                "Он сжал кольцо в кулаке.\n\n" +
                "— С этим я сам разберусь.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.RemoveItem("signet_ring"),
                    Effect.AddInformation(
                        "ring_taken_note", "Руперт забрал кольцо",
                        "Стражник забрал у вас кольцо, заподозрив в мародёрстве, и заявил, что " +
                                "разберётся с находкой сам.",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.Transition("c1_last_guard_talk")
                )
            )
        )
    )

    private val askRingFailureScene = Scene(
        id = "c1_ask_ring_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "СПРОСИТЬ О КОЛЬЦЕ-ПЕЧАТКЕ: ПРОВАЛ",
        text = "Вы впервые видите такой замысловатый узор на кольце.\n\n" +
                "На тёмном металле тонкой работы переплетаются линии герба. Несмотря на грязь и " +
                "потёртости, мастерство заметно даже спустя годы.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_guard_ring_reaction"))
            )
        )
    )

    private val askRingSuccessScene = Scene(
        id = "c1_ask_ring_success",
        locationId = "lower_city_alley",
        consequenceTitle = "СПРОСИТЬ О КОЛЬЦЕ-ПЕЧАТКЕ: УСПЕХ",
        text = "Вам уже знаком этот герб, вы видели его в бумагах — это герб покойного Гремма.\n\n" +
                "Должно быть, кольцо слетело с его пальца, с того самого, которого стражники так и " +
                "не нашли.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.AddInformation(
                        "ring_gremm_crest", "Кольцо с гербом убитого",
                        "Герб на кольце знаком по документам — это герб покойного Гремма. Должно " +
                                "быть, кольцо слетело с его пальца — того самого, которого стражники " +
                                "так и не нашли.",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.Transition("c1_guard_ring_reaction")
                )
            )
        )
    )

    private val askRingCritSuccessScene = Scene(
        id = "c1_ask_ring_crit_success",
        locationId = "lower_city_alley",
        consequenceTitle = "СПРОСИТЬ О КОЛЬЦЕ-ПЕЧАТКЕ: КРИТИЧЕСКИЙ УСПЕХ",
        text = "Вам уже знаком этот герб, вы видели его в бумагах — это герб покойного Гремма.\n\n" +
                "Должно быть, кольцо слетело с его пальца, с того самого, которого стражники так и " +
                "не нашли.\n\n" +
                "На внутренней стороне печатки виднеются цифры: 3, 8 и 5.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.AddInformation(
                        "ring_gremm_crest", "Кольцо с гербом убитого",
                        "Герб на кольце знаком по документам — это герб покойного Гремма. Должно " +
                                "быть, кольцо слетело с его пальца — того самого, которого стражники " +
                                "так и не нашли.",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.AddInformation(
                        "ring_numbers", "Странные цифры на кольце",
                        "На внутренней стороне печатки виднеются цифры: 3, 8 и 5.",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.Transition("c1_guard_ring_reaction")
                )
            )
        )
    )

    private val lastGuardTalk = Scene(
        id = "c1_last_guard_talk",
        locationId = "lower_city_alley",
        speakerName = "Стражник Руперт",
        text = "Руперт рассмеялся, явно наслаждаясь собственным превосходством.\n\n" +
                "— Мы же люди взрослые. Предлагаю договориться.\n\n" +
                "Стражник протянул руку.\n\n" +
                "— Колечко останется у меня. Считай, презент. Это будет наш маленький секрет.\n\n" +
                "Он наклонился ближе.\n\n" +
                "— А ты сгинешь с глаз моих. Мне всё равно куда, лишь бы больше тебя здесь не видеть.\n\n" +
                "Стражник снова натянул на лицо неприятную улыбку.\n\n" +
                "— Ах да. Если сделаешь небольшое пожертвование корпусу стражи...\n\n" +
                "Он многозначительно посмотрел на вас.\n\n" +
                "— Я могу поделиться кое-какими сведениями о доме Гремма. Может, найдёшь там себе " +
                "ещё что-нибудь из украшений.\n\n" +
                "Руперт убрал кольцо за пояс.\n\n" +
                "— В конце концов, раз уж ты такой любитель чужих тайн, почему бы не заработать на них?",
        actions = listOf(
            Action(
                id = "pay_for_address",
                text = "Заплатить стражнику за информацию о доме убитого",
                condition = Condition.ResourceCompare("gold", Condition.Comparison.AT_LEAST, 4),
                effects = listOf(
                    Effect.ChangeResource("gold", -4, "Золото", min = 0),
                    Effect.AddInformation("house_385", "Дом № 385", "Адрес дома покойного Гремма: дом № 385, Главная улица.", "Дело", InformationType.GOAL, linkIfPresent = "ring_numbers"),
                    Effect.Transition("c1_alley_entrance")
                )
            )
        )
    )

    private val chanceryRing = Scene(
        id = "c1_chancery_ring",
        locationId = "main_square",
        text = "С Нижнего города вы направились в городскую канцелярию. Перстень с гербом приняли " +
                "без особого удивления, но после короткого осмотра служащий признал, что определить " +
                "его происхождение сразу не сможет.\n\n" +
                "За два золотых монеты канцелярия была готова провести поиск по архивам, сопоставить " +
                "герб со старыми городскими записями и установить, кому принадлежала печатка.\n\n" +
                "Служащий записал заказ в книгу и забрал перстень.\n\n" +
                "— Приходите завтра. Если герб где-нибудь значится, мы его найдём.",
        actions = listOf(
            Action(
                id = "pay_archive",
                text = "Заплатить за работу в архиве",
                condition = Condition.ResourceCompare("gold", Condition.Comparison.AT_LEAST, 2),
                effects = listOf(Effect.ChangeResource("gold", -2, "Золото", min = 0), Effect.Transition("c1_chancery_ring_result"))
            )
        )
    )

    private val chanceryRingResult = Scene(
        id = "c1_chancery_ring_result",
        locationId = "main_square",
        text = "На следующий день в канцелярии герою сообщили результаты поиска.\n\n" +
                "Перстень принадлежал Эдварду Гремму. Герб на печатке совпадал с гербом, указанным в " +
                "старых городских записях. В архиве также нашли его адрес — дом № 385 на Главной " +
                "улице.\n\n" +
                "Но вместе с этим выяснилось и другое: Гремм умер семь лет назад. В доме после его " +
                "смерти никто не поселился, во всяком случае бумаг о заселении в дом № 385 в " +
                "канцелярию не подавали. Наследников у него не осталось. Заявлений о пропаже перстня " +
                "за эти годы в канцелярии тоже не было.\n\n" +
                "Служащий перелистнул записи и пожал плечами.\n\n" +
                "— Формально искать владельца уже некому. Можете оставить перстень себе.",
        actions = listOf(
            Action(
                id = "go_to_house_from_chancery",
                text = "Отправиться в дом убитого",
                effects = listOf(
                    Effect.AddInformation("house_385", "Дом № 385", "Адрес дома покойного Гремма: дом № 385, Главная улица.", "Дело", InformationType.GOAL, linkIfPresent = "ring_numbers"),
                    Effect.Transition("c1_house_385", "main_street")
                )
            )
        )
    )

    private val house385 = Scene(
        id = "c1_house_385",
        locationId = "main_street",
        text = "Дом № 385 на Главной улице.\n\n" +
                "[Заглушка — текст этой сцены ещё не прислан. Дальше здесь продолжится расследование.]",
        actions = listOf(
            Action(id = "examine_house_stub", text = "Осмотреть дом")
        )
    )

    private val examineRingCritFailureScene = Scene(
        id = "c1_examine_ring_crit_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "РАССМОТРЕТЬ КОЛЬЦО-ПЕЧАТКУ: КРИТИЧЕСКИЙ ПРОВАЛ",
        speakerName = "Стражник Руперт",
        text = "Руперт уверенно подбросил кольцо вверх и поймал его на ладонь.\n\n" +
                "— Это улика. Достойная находка достойного солдата.\n\n" +
                "Он спрятал кольцо в кулак.\n\n" +
                "— Я её нашёл, я и отчитаюсь перед начальством.\n\n" +
                "Руперт указал на выход из переулка.\n\n" +
                "— А ты пошёл вон.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.RemoveItem("signet_ring"),
                    Effect.AddInformation(
                        "ring_confiscated_note", "Руперт присвоил кольцо",
                        "Стражник присвоил кольцо, заявив, что разберётся с находкой сам.",
                        "Дело", InformationType.NOTE
                    ),
                    Effect.Transition("c1_last_guard_talk")
                )
            )
        )
    )

    private val examineRingFailureScene = Scene(
        id = "c1_examine_ring_failure",
        locationId = "lower_city_alley",
        consequenceTitle = "РАССМОТРЕТЬ КОЛЬЦО-ПЕЧАТКУ: ПРОВАЛ",
        speakerName = "Стражник Руперт",
        text = "— Впервые такое вижу, — сказал Руперт.\n\n" +
                "Он прищурился, разглядывая кольцо.\n\n" +
                "— Может, кто обронил. Хотя кто станет ронять золотой перстень в Нижнем городе?\n\n" +
                "Стражник оскалился, обнажив гнилые зубы.\n\n" +
                "— Отнеси его в канцелярию. Может, местные лизоблюды мэра тебе помогут.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(Effect.Transition("c1_chancery_ring", "main_square"))
            )
        )
    )

    private val examineRingSuccessScene = Scene(
        id = "c1_examine_ring_success",
        locationId = "lower_city_alley",
        consequenceTitle = "РАССМОТРЕТЬ КОЛЬЦО-ПЕЧАТКУ: УСПЕХ",
        speakerName = "Стражник Руперт",
        text = "Руперт ещё раз взглянул на кольцо.\n\n" +
                "— Так это же перстень Гремма.\n\n" +
                "На его лице появилось беспокойство.\n\n" +
                "— Странно, его ведь вроде схоронили вместе с ним ещё семь лет назад.\n\n" +
                "Он потупил взгляд.\n\n" +
                "— Точно чертовщина.\n\n" +
                "Руперт вернул кольцо.\n\n" +
                "— Я бы на твоём месте держался подальше и от него, и от дома его хозяина. Но если " +
                "всё же захочешь туда заглянуть — пеняй на себя.\n\n" +
                "Он махнул рукой.\n\n" +
                "— Адрес тебе скажу, так и быть. Но больше ничего не знаю. Всё, иди.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.AddInformation(
                        "house_385", "Дом № 385",
                        "Адрес дома покойного Гремма: дом № 385, Главная улица.",
                        "Дело", InformationType.GOAL, linkIfPresent = "ring_numbers"
                    ),
                    Effect.Transition("c1_alley_entrance")
                )
            )
        )
    )

    private val examineRingCritSuccessScene = Scene(
        id = "c1_examine_ring_crit_success",
        locationId = "lower_city_alley",
        consequenceTitle = "РАССМОТРЕТЬ КОЛЬЦО-ПЕЧАТКУ: КРИТИЧЕСКИЙ УСПЕХ",
        speakerName = "Стражник Руперт",
        text = "— Так это же перстень Гремма.\n\n" +
                "Руперт несколько секунд смотрел на него, затем усмехнулся.\n\n" +
                "— Странно. Его ведь похоронили вместе с Эдвардом семь лет назад.\n\n" +
                "Он покачал головой.\n\n" +
                "— Точно чертовщина.\n\n" +
                "Руперт достал из кармана ключ и покрутил его перед вами.\n\n" +
                "— А вот это тебе, должно быть, пригодится. Ключ от его дома. Лежал у него в кармане.\n\n" +
                "Он отдал ключ и довольно улыбнулся.\n\n" +
                "— Не спрашивай, откуда он у меня. Честные стражники по карманам мёртвых не лазят.\n\n" +
                "Он выдержал паузу.\n\n" +
                "— Адрес, наверное, у тебя в бумагах уже размылся. Дом номер триста восемьдесят " +
                "пять, Главная улица.",
        actions = listOf(
            Action(
                id = "continue",
                text = "Продолжить",
                effects = listOf(
                    Effect.AddInformation(
                        "house_385", "Дом № 385",
                        "Адрес дома покойного Гремма: дом № 385, Главная улица.",
                        "Дело", InformationType.GOAL, linkIfPresent = "ring_numbers"
                    ),
                    Effect.AddItem(
                        "house_385_key", "Ключ от дома № 385",
                        "Ключ, который Руперт, судя по всему, стащил из карманов покойного.", 1
                    ),
                    Effect.Transition("c1_alley_entrance")
                )
            )
        )
    )

    val itemExamineActions: Map<String, Action> = mapOf(
        "victim_documents" to Action(
            id = "examine_documents",
            text = "Рассмотреть документы",
            effects = listOf(
                Effect.AddInformation(
                    "docs_illegible", "Документы промокли",
                    "К сожалению, документы прочитать слишком сложно — чернила в них потекли. " +
                            "Видимо, стражник простоял с ними всю эту дождливую ночь.",
                    "Дело", InformationType.NOTE
                ),
                Effect.AddInformation(
                    "address_hard_to_find", "Адрес найти будет непросто",
                    "Найти адрес в бумагах сложно — нужно постараться найти иной способ его узнать.",
                    "Дело", InformationType.GOAL
                )
            )
        ),
        "signet_ring" to Action(
            id = "examine_ring",
            text = "Рассмотреть кольцо-печатку",
            check = Check(StatType.INTELLECT, SkillType.KNOWLEDGE, 18),
            consequences = mapOf(
                CheckOutcome.CRITICAL_SUCCESS to "c1_examine_ring_crit_success",
                CheckOutcome.SUCCESS to "c1_examine_ring_success",
                CheckOutcome.FAILURE to "c1_examine_ring_failure",
                CheckOutcome.CRITICAL_FAILURE to "c1_examine_ring_crit_failure"
            )
        )
    )

    val startingScene = alleyEntrance

    val scenes: List<Scene> = listOf(
        alleyEntrance, guardDialogue, guardRecognized, guardRecognizedAnnoyed,
        guardIncident, guardDocuments, guardSnide, guardEvidence, guardStrangeness,
        guardFinger, guardAngry, guardRats, crimeScene,
        guardRingReaction, lastGuardTalk, chanceryRing, chanceryRingResult, house385,
        examineGuardCritSuccessScene, examineGuardSuccessScene,
        examineGuardFailureScene, examineGuardCritFailureScene,
        askMoreCritFailureScene, askMoreFailureScene,
        askMoreSuccessScene, askMoreCritSuccessScene,
        nervousCritFailureScene, nervousFailureScene,
        nervousSuccessScene, nervousCritSuccessScene,
        ivyCritFailureScene, ivyFailureScene,
        ivySuccessScene, ivyCritSuccessScene,
        pavementCritFailureScene, pavementFailureScene,
        pavementSuccessScene, pavementCritSuccessScene,
        askRingCritFailureScene, askRingFailureScene,
        askRingSuccessScene, askRingCritSuccessScene,
        examineRingCritFailureScene, examineRingFailureScene,
        examineRingSuccessScene, examineRingCritSuccessScene
    )
}