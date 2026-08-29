package com.jostein.maratontrener.repository;

import com.jostein.maratontrener.models.Ingredient;
import com.jostein.maratontrener.models.Meal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MealRepository {

    private static final List<Meal> ALL_MEALS = new ArrayList<>();

    static {
        ALL_MEALS.add(new Meal(
            "w1_frokost", "Kyllingwok med brokkoli og cashewnøtter", "Middag 1", 1,
            Arrays.asList(
                new Ingredient("Kyllingfilet (i strimler)", 150, "g"),
                new Ingredient("Brokkoli (i buketter)", 100, "g"),
                new Ingredient("Woksaus (lavorientert)", 2, "ss"),
                new Ingredient("Cashewnøtter (usaltede)", 20, "g"),
                new Ingredient("Fullkornsris (tørrvekt)", 80, "g")
            ),
            "Wok kyllingstrimler og brokkoli i litt olje på sterk varme. Tilsett woksaus og rør inn cashewnøtter til slutt. Server med kokt fullkornsris.",
            520, 38, 55, 16,
            "Kostråd #1: Bruk rikelig med grønnsaker i woken for vitaminer og fiber.",
            "🍲"
        ));

        ALL_MEALS.add(new Meal(
            "w1_lunsj", "Kremet pasta med laks og spinat", "Middag 2", 1,
            Arrays.asList(
                new Ingredient("Laksefilet uten skinn", 120, "g"),
                new Ingredient("Fullkornspasta", 80, "g"),
                new Ingredient("Matfløte (lett)", 1, "dl"),
                new Ingredient("Frisk spinat", 50, "g"),
                new Ingredient("Hvitløk", 1, "fedd")
            ),
            "Kok pasta. Stek lakseterninger og hakket hvitløk raskt. Tilsett matfløte og la det småkoke. Vend inn spinat og pasta til spinaten faller sammen.",
            610, 32, 62, 26,
            "Kostråd #3: Fet fisk som laks gir viktige Omega-3 fettsyrer for leddene.",
            "🍝"
        ));

        ALL_MEALS.add(new Meal(
            "w1_middag", "Ovnsbakt laks med brokkoli og fullkornsris", "Middag 3", 1,
            Arrays.asList(
                new Ingredient("Laksefilet uten skinn", 150, "g"),
                new Ingredient("Brokkoli (i buketter)", 120, "g"),
                new Ingredient("Fullkornsris (tørrvekt)", 80, "g"),
                new Ingredient("Rapsolje", 1, "ss")
            ),
            "Kok ris etter anvisning på pakken. Legg laksefilet og brokkoli i en ildfast form. Drypp rapsolje over, og krydre med litt salt og pepper. Bak i stekeovn på 200 °C i ca 12-15 minutter.",
            650, 42, 62, 22,
            "Kostråd #3: Spis minst 300-450g fisk i uken, hvorav minst 200g bør være fet fisk som laks.",
            "🍣"
        ));

        ALL_MEALS.add(new Meal(
            "w2_frokost", "Stekt ris med kylling og grønnsaker", "Middag 1", 2,
            Arrays.asList(
                new Ingredient("Kyllingfilet (i terninger)", 120, "g"),
                new Ingredient("Kokt ris (kald ris fungerer best)", 150, "g"),
                new Ingredient("Erter og gulrøtter", 100, "g"),
                new Ingredient("Egg", 1, "stk"),
                new Ingredient("Soyasaus (redusert salt)", 1, "ss")
            ),
            "Stek kyllingstrimler i en panne. Tilsett ris og grønnsaker, stek i noen minutter. Skyv alt til side og rør inn egget til det stivner. Smak til med soyasaus.",
            490, 35, 52, 12,
            "Kostråd #2: Rester av ris egner seg perfekt til en rask stekt ris dagen etter.",
            "🍛"
        ));

        ALL_MEALS.add(new Meal(
            "w2_lunsj", "Quinoasalat med kyllingfilet", "Middag 2", 2,
            Arrays.asList(
                new Ingredient("Quinoa (kokt)", 150, "g"),
                new Ingredient("Grillet kyllingfilet", 100, "g"),
                new Ingredient("Cherrytomater", 60, "g"),
                new Ingredient("Olivenolje til dressing", 1, "ss")
            ),
            "Bland kokt quinoa med kylling i strimler og delte cherrytomater. Ringle over litt olivenolje, sitronsaft, salt og pepper.",
            460, 32, 45, 14,
            "Kostråd #3: Velg gjerne hvitt kjøtt (kylling) fremfor rødt kjøtt.",
            "🥗"
        ));

        ALL_MEALS.add(new Meal(
            "w2_middag", "Klassisk linsesuppe med grovbrød", "Middag 3", 2,
            Arrays.asList(
                new Ingredient("Røde linser (tørre)", 90, "g"),
                new Ingredient("Hakkede hermetiske tomater", 200, "g"),
                new Ingredient("Gulrot og løk", 100, "g"),
                new Ingredient("Grovt brød til servering", 1, "skive")
            ),
            "Fres hakket løk og gulrot i litt olje. Tilsett skylte linser, hermetiske tomater og 4 dl vann med grønnsaksbuljong. La suppen koke i 15-20 minutter til linsene er møre. Server med grovbrød.",
            510, 26, 75, 6,
            "Kostråd #3: Velg bønner og linser som proteinkilder oftere.",
            "🥣"
        ));

        ALL_MEALS.add(new Meal(
            "w3_frokost", "Meksikansk bønnegryte med avokado", "Middag 1", 3,
            Arrays.asList(
                new Ingredient("Svarte bønner (hermetiske)", 150, "g"),
                new Ingredient("Hakkede tomater", 200, "g"),
                new Ingredient("Avokado", 0.5, "stk"),
                new Ingredient("Frisk koriander", 5, "g"),
                new Ingredient("Fullkornsris (tørrvekt)", 70, "g")
            ),
            "Varm bønner, hermetiske tomater og spisskummen i en kjele. Server over kokt fullkornsris og topp med ferske avokadoskiver og koriander.",
            470, 16, 68, 14,
            "Kostråd #3: Belgfrukter og avokado gir sunt plantefett og rikelig med kostfiber.",
            "🍲"
        ));

        ALL_MEALS.add(new Meal(
            "w3_lunsj", "Kyllingwrap med tzatziki og salat", "Middag 2", 3,
            Arrays.asList(
                new Ingredient("Fullkornswrap / tortilla", 1, "stk"),
                new Ingredient("Kyllingfilet (i strimler)", 120, "g"),
                new Ingredient("Mager yoghurt og agurk", 100, "g"),
                new Ingredient("Blandet salat", 30, "g")
            ),
            "Stek kyllingstrimlene i litt rapsolje. Bland agurkskiver og yoghurt til en enkel tzatziki. Fyll tortillaen med kylling, tzatziki og salat, og rull sammen.",
            420, 34, 36, 12,
            "Kostråd #4: Meieriprodukter med mindre fett (som mager yoghurt) anbefales til hverdags.",
            "🌯"
        ));

        ALL_MEALS.add(new Meal(
            "w3_middag", "Torsk i form med purre og poteter", "Middag 3", 3,
            Arrays.asList(
                new Ingredient("Torskefilet", 180, "g"),
                new Ingredient("Purre og tomat", 120, "g"),
                new Ingredient("Kokte poteter", 200, "g"),
                new Ingredient("Olivenolje", 1, "ss")
            ),
            "Legg torskestykket i en form sammen med snittet purre og delte tomater. Drypp olivenolje over og krydre. Bak på 180 °C i 15 minutter. Server med kokte poteter.",
            480, 38, 40, 12,
            "Kostråd #3: Fisk og sjømat bidrar med viktige proteiner og mineraler.",
            "🐟"
        ));

        ALL_MEALS.add(new Meal(
            "w4_frokost", "Pasta med linsebolognese", "Middag 1", 4,
            Arrays.asList(
                new Ingredient("Røde linser (skylt)", 80, "g"),
                new Ingredient("Hakkede tomater", 200, "g"),
                new Ingredient("Fullkornsspagetti", 80, "g"),
                new Ingredient("Løk og gulrot (hakket)", 80, "g")
            ),
            "Kok spagetti. Fres hakket løk og gulrot, tilsett linser og hermetiske tomater, og la det småkoke i 15 minutter til linsene er møre. Server sausen over pastaen.",
            510, 22, 85, 4,
            "Kostråd #2: Fullkornspasta og linser gir langvarig energi og verdifullt jern.",
            "🍝"
        ));

        ALL_MEALS.add(new Meal(
            "w4_lunsj", "Kyllingsalat med kikerter og pesto", "Middag 2", 4,
            Arrays.asList(
                new Ingredient("Kyllingfilet (i terninger)", 100, "g"),
                new Ingredient("Kikerter (hermetiske)", 100, "g"),
                new Ingredient("Grønn pesto", 1, "ss"),
                new Ingredient("Cherrytomater", 60, "g"),
                new Ingredient("Salatblader / ruccola", 40, "g")
            ),
            "Stek kyllingfilet i terninger. Bland kylling, kikerter, pesto, cherrytomater og salat sammen i en stor skål.",
            440, 32, 30, 18,
            "Kostråd #1: Grønnsaker og sunt fett fra pesto forebygger betennelse under hard trening.",
            "🥗"
        ));

        ALL_MEALS.add(new Meal(
            "w4_middag", "Kyllingwok med grønnsaker og fullkorns-nudler", "Middag 3", 4,
            Arrays.asList(
                new Ingredient("Kyllingfilet (i strimler)", 140, "g"),
                new Ingredient("Wokgrønnsaker (gulrot, paprika, løk)", 150, "g"),
                new Ingredient("Fullkorns-nudler (tørr)", 70, "g"),
                new Ingredient("Solsikkeolje til steking", 1, "ss")
            ),
            "Kok nudlene. Stek kyllingstrimlene i olje i en varm wok eller panne. Tilsett grønnsakene og stek videre i 4 minutter. Vend inn de kokte nudlene og litt soyasaus.",
            590, 40, 68, 13,
            "Kostråd #3: Velg hvitt kjøtt fremfor rødt kjøtt, og fyll tallerkenen med grønnsaker.",
            "🍜"
        ));

        ALL_MEALS.add(new Meal(
            "w5_frokost", "Ovnsbakt torsk med søtpotetmos", "Middag 1", 5,
            Arrays.asList(
                new Ingredient("Torskefilet", 160, "g"),
                new Ingredient("Søtpotet", 200, "g"),
                new Ingredient("Brokkoli (i buketter)", 100, "g"),
                new Ingredient("Flytende margarin", 1, "ss")
            ),
            "Bak torskefileten i ovnen på 180 °C i 15 minutter. Kok søtpotet og mos den med litt salt, pepper og margarin. Server med dampet brokkoli.",
            450, 34, 45, 11,
            "Kostråd #1: Søtpotet er stappfull av betakaroten som omdannes til vitamin A i kroppen.",
            "🐟"
        ));

        ALL_MEALS.add(new Meal(
            "w5_lunsj", "Laksesalat med quinoa og fetaost", "Middag 2", 5,
            Arrays.asList(
                new Ingredient("Laksefilet (stekt)", 100, "g"),
                new Ingredient("Quinoa (kokt)", 100, "g"),
                new Ingredient("Fetaost (blokk)", 20, "g"),
                new Ingredient("Agurk og tomat", 80, "g")
            ),
            "Stek laksen og del den i biter. Kok quinoa. Bland laks, quinoa, fetaostterninger, agurk og tomat i en bolle.",
            530, 28, 36, 25,
            "Kostråd #3: Lakseolje sikrer gode fettsyrer som beskytter sener og muskler mot belastning.",
            "🥗"
        ));

        ALL_MEALS.add(new Meal(
            "w5_middag", "Bønnetaco med guacamole", "Middag 3", 5,
            Arrays.asList(
                new Ingredient("Svarte bønner (hermetiske)", 150, "g"),
                new Ingredient("Tacoskjell eller grove tortillas", 2, "stk"),
                new Ingredient("Avokado til guacamole", 0.5, "stk"),
                new Ingredient("Salat, tomat og mais", 100, "g")
            ),
            "Varm bønnene med tacokrydder. Lag guacamole av avokado, sitronsaft, salt og hvitløk. Fyll skjellene med bønner, grønnsaker og guacamole.",
            540, 18, 64, 20,
            "Kostråd #3: Kutt ned på kjøttdeig av storfekjøtt og velg næringsrike bønner.",
            "🌮"
        ));

        ALL_MEALS.add(new Meal(
            "w6_frokost", "Hjemmelaget sunn fiskegrateng", "Middag 1", 6,
            Arrays.asList(
                new Ingredient("Torskefilet (i biter)", 120, "g"),
                new Ingredient("Fullkornsmakaroni", 50, "g"),
                new Ingredient("Mager hvit saus", 1.5, "dl"),
                new Ingredient("Strøkavring / brødrasp", 10, "g")
            ),
            "Kok makaroni. Legg torskebiter og makaroni i en form, hell over hvit saus og strø over kavring. Stek på 200 °C i ca 25 minutter.",
            480, 32, 48, 12,
            "Kostråd #3: Torsk bidrar med jod og magre proteiner for oppbygging av musklene.",
            "🍲"
        ));

        ALL_MEALS.add(new Meal(
            "w6_lunsj", "Pasta carbonara med kalkunbacon", "Middag 2", 6,
            Arrays.asList(
                new Ingredient("Fullkornspasta", 80, "g"),
                new Ingredient("Kalkunbacon (terninger)", 50, "g"),
                new Ingredient("Egg", 1, "stk"),
                new Ingredient("Parmesan (revet)", 10, "g"),
                new Ingredient("Lettmelk", 2, "ss")
            ),
            "Kok pasta. Stek bacon. Pisk egg, parmesan og melk i en kopp. Bland avrent pasta med bacon, ta pannen av varmen og hell i eggeblandingen. Rør raskt.",
            540, 28, 62, 16,
            "Kostråd #4: Bruk magre kjøttalternativer som kalkunbacon for å spare mettet fett.",
            "🍝"
        ));

        ALL_MEALS.add(new Meal(
            "w6_middag", "Stekt sei med råkost og potet", "Middag 3", 6,
            Arrays.asList(
                new Ingredient("Seifilet (uten skinn)", 160, "g"),
                new Ingredient("Råkostsalat (gulrot/kål)", 130, "g"),
                new Ingredient("Kokt potet", 200, "g"),
                new Ingredient("Flytende margarin til steking", 1, "ss")
            ),
            "Vend seifiletene i litt grovt mel, salt og pepper. Stek i margarin i ca 3-4 minutter på hver side. Server med råkostsalat (skvis sitron over) og kokte poteter.",
            520, 36, 46, 15,
            "Kostråd #3: Sei er en rimelig og mager proteinkilde, rik på jod.",
            "🐟"
        ));

        ALL_MEALS.add(new Meal(
            "w7_frokost", "Linsesuppe med lett kokosmelk", "Middag 1", 7,
            Arrays.asList(
                new Ingredient("Røde linser (tørre)", 80, "g"),
                new Ingredient("Kokosmelk (lett)", 1, "dl"),
                new Ingredient("Hakkede hermetiske tomater", 150, "g"),
                new Ingredient("Ingefær og hvitløk", 5, "g"),
                new Ingredient("Gulrot (i biter)", 1, "stk")
            ),
            "Fres ingefær og hvitløk. Tilsett gulrot, skylte linser, tomater og kokosmelk. La småkoke i 20 minutter til linsene er møre.",
            430, 18, 48, 14,
            "Kostråd #3: Ingefær og hvitløk styrker immunforsvaret i tunge treningsuker.",
            "🥣"
        ));

        ALL_MEALS.add(new Meal(
            "w7_lunsj", "Speltlompe-pizza med skinke og mozzarella", "Middag 2", 7,
            Arrays.asList(
                new Ingredient("Speltlomper", 3, "stk"),
                new Ingredient("Tomatpuré / pizzasaus", 3, "ss"),
                new Ingredient("Kokt skinke (strimlet)", 60, "g"),
                new Ingredient("Revet lettost eller mozzarella", 40, "g"),
                new Ingredient("Oregano", 2, "g")
            ),
            "Legg lompene på bakepapir. Smør pizzasaus utover, fordel skinke og lettost. Stek i ovnen på 200 °C i ca 8-10 minutter.",
            390, 28, 38, 11,
            "Kostråd #2: Speltlompe-pizza er et raskt og fiberrikt alternativ til vanlig pizzabunn.",
            "🍕"
        ));

        ALL_MEALS.add(new Meal(
            "w7_middag", "Kyllinggryte med rotgrønnsaker", "Middag 3", 7,
            Arrays.asList(
                new Ingredient("Kyllingfilet (i terninger)", 150, "g"),
                new Ingredient("Søtpotet, gulrot og kålrot", 180, "g"),
                new Ingredient("Hakkede tomater", 200, "g"),
                new Ingredient("Olivenolje", 1, "ss")
            ),
            "Fres kyllingterningene i olivenolje i en gryte. Tilsett rotgrønnsaker i biter og hell over hakkede tomater og 2 dl vann. La gryten småkoke i ca 20 minutter til grønnsakene er møre.",
            550, 38, 55, 14,
            "Kostråd #3: Kylling og rotgrønnsaker gir utmerket restitusjonsmat.",
            "🍲"
        ));

        ALL_MEALS.add(new Meal(
            "w8_frokost", "Havrepannekaker med speilegg og kalkun", "Middag 1", 8,
            Arrays.asList(
                new Ingredient("Havremel (eller havregryn)", 80, "g"),
                new Ingredient("Egg (til røre og steking)", 2, "stk"),
                new Ingredient("Lettmelk", 1.5, "dl"),
                new Ingredient("Kalkunpålegg (skiver)", 4, "stk")
            ),
            "Lag røre av havremel, ett egg og melk. Stek pannekaker. Stek det andre egget som speilegg. Rull pannekakene med speilegg, kalkun og ruccolasalat.",
            460, 26, 56, 12,
            "Kostråd #2: Protein og komplekse karbohydrater gir jevn energifrigjøring.",
            "🥞"
        ));

        ALL_MEALS.add(new Meal(
            "w8_lunsj", "Kylling- og quinoasalat med rapsolje", "Middag 2", 8,
            Arrays.asList(
                new Ingredient("Grillet kyllingfilet", 100, "g"),
                new Ingredient("Quinoa (kokt)", 120, "g"),
                new Ingredient("Paprika, agurk og mais", 80, "g"),
                new Ingredient("Rapsolje og sitronsaft", 1, "ss")
            ),
            "Hakk kylling og grønnsaker. Bland alt med kokt quinoa i en skål, og ringle over rapsolje og sitron.",
            450, 30, 42, 14,
            "Kostråd #1: Quinoa er en av få plantekilder med komplette aminosyrer.",
            "🥗"
        ));

        ALL_MEALS.add(new Meal(
            "w8_middag", "Stekt ørret med agurksalat og potet", "Middag 3", 8,
            Arrays.asList(
                new Ingredient("Ørretfilet", 150, "g"),
                new Ingredient("Agurk (i tynne skiver)", 100, "g"),
                new Ingredient("Kokte poteter", 180, "g"),
                new Ingredient("Rømme (lettrømme 10%)", 2, "ss")
            ),
            "Stek eller bak ørreten i ovnen. Lag en rask agurksalat med eddik, vann, ørlite sukker, salt og pepper. Server med kokte nypoteter og en klatt lettrømme.",
            620, 36, 42, 28,
            "Kostråd #3: Ørret gir sunne marine fettsyrer som styrker ledd og hjerte.",
            "🐟"
        ));

        ALL_MEALS.add(new Meal(
            "w9_frokost", "Chili con carne med kylling og kikerter", "Middag 1", 9,
            Arrays.asList(
                new Ingredient("Kyllingkjøttdeig", 120, "g"),
                new Ingredient("Kikerter (hermetiske)", 100, "g"),
                new Ingredient("Hakkede tomater", 200, "g"),
                new Ingredient("Spisskummen, paprika og chili", 2, "g"),
                new Ingredient("Fullkornsris (tørrvekt)", 70, "g")
            ),
            "Stek kjøttdeigen. Ha i tomater, kikerter og krydder, la småkoke i 10 minutter. Server rykende varm med kokt ris.",
            580, 38, 68, 12,
            "Kostråd #3: Kyllingkjøttdeig er et fettfattig alternativ til storfekjøtt.",
            "🍲"
        ));

        ALL_MEALS.add(new Meal(
            "w9_lunsj", "Laksewrap med avokadokrem", "Middag 2", 9,
            Arrays.asList(
                new Ingredient("Grov tortilla / wrap", 1, "stk"),
                new Ingredient("Røkt laks (i skiver)", 60, "g"),
                new Ingredient("Avokado", 0.5, "stk"),
                new Ingredient("Frisk spinat / ruccola", 30, "g")
            ),
            "Mos avokado med litt sitron og salt. Smør kremen på tortillaen, legg på laks og spinat, rull sammen.",
            410, 20, 32, 22,
            "Kostråd #3: Laks og avokado smører leddene med sunt, umettet fett.",
            "🌯"
        ));

        ALL_MEALS.add(new Meal(
            "w9_middag", "Søtpotetsuppe med sprøstekte kikerter", "Middag 3", 9,
            Arrays.asList(
                new Ingredient("Søtpotet (i terninger)", 200, "g"),
                new Ingredient("Gulrot (i biter)", 100, "g"),
                new Ingredient("Kikerter (hermetiske, bakt i ovn)", 80, "g"),
                new Ingredient("Olivenolje", 1, "ss")
            ),
            "Kok søtpotet og gulrot møre i grønnsaksbuljong, kjør glatt med en stavmikser. Kikerter tørkes, vendes i litt olje og krydder, og bakes i stekeovn på 200 °C i 20 minutter til de er sprø. Topp suppen med kikertene.",
            540, 16, 78, 14,
            "Kostråd #1: Grønnsaker og rotfrukter gir rikelig med vitamin A og C.",
            "🍜"
        ));

        ALL_MEALS.add(new Meal(
            "w10_frokost", "Stekt sei med gyllen løk og potetmos", "Middag 1", 10,
            Arrays.asList(
                new Ingredient("Seifilet (uten skinn)", 150, "g"),
                new Ingredient("Løk (i ringer)", 1, "stk"),
                new Ingredient("Poteter", 200, "g"),
                new Ingredient("Lettmelk til potetmos", 0.5, "dl")
            ),
            "Stek sei og løk i en panne. Kok poteter og mos med melk, litt salt og pepper til en jevn mos. Server sammen.",
            460, 34, 48, 10,
            "Kostråd #3: Sei gir masse fullverdig protein og er svært fattig på mettet fett.",
            "🐟"
        ));

        ALL_MEALS.add(new Meal(
            "w10_lunsj", "Tuna melt (varmt tunfisksmørbrød)", "Middag 2", 10,
            Arrays.asList(
                new Ingredient("Grovt brød (skiver)", 2, "skiver"),
                new Ingredient("Tunfisk i vann (boks, avrent)", 90, "g"),
                new Ingredient("Lettmajones", 1, "ss"),
                new Ingredient("Revet lettost", 30, "g")
            ),
            "Bland tunfisk og majones. Fordel på brødskivene, legg ost over, og gratiner i stekeovn på 200 °C til osten er gyllen.",
            480, 35, 32, 18,
            "Kostråd #3: Tunfisk er en rimelig og proteinrik klassiker for aktive løpere.",
            "🥪"
        ));

        ALL_MEALS.add(new Meal(
            "w10_middag", "Lakseburgere i grovt burgerbrød", "Middag 3", 10,
            Arrays.asList(
                new Ingredient("Lakseburger (rå laksefarse)", 130, "g"),
                new Ingredient("Grovt burgerbrød", 1, "stk"),
                new Ingredient("Råkost / salat", 50, "g"),
                new Ingredient("Mager hvitløksdressing", 1, "ss")
            ),
            "Stek lakseburgeren i en middels varm panne med litt olje i ca 3 minutter på hver side. Varm burgerbrødet, ha på dressing, råkostsalat og lakseburgeren.",
            530, 32, 48, 19,
            "Kostråd #3: Fiskeburgere med høy andel fisk er et sunt og barnevennlig valg.",
            "🍔"
        ));

        ALL_MEALS.add(new Meal(
            "w11_frokost", "Pasta med kylling, hvitløk og tomat", "Middag 1", 11,
            Arrays.asList(
                new Ingredient("Fullkornspasta", 80, "g"),
                new Ingredient("Kyllingfilet (i biter)", 120, "g"),
                new Ingredient("Hakkede tomater", 150, "g"),
                new Ingredient("Hvitløk", 1, "fedd"),
                new Ingredient("Rapsolje", 1, "ss")
            ),
            "Kok pasta. Stek kylling og hvitløk i olje. Tilsett tomater, og la sausen småkoke litt. Bland sammen med pastaen.",
            530, 36, 62, 12,
            "Kostråd #2: Fullkornspasta fyller opp glykogenlagrene etter hard trening.",
            "🍝"
        ));

        ALL_MEALS.add(new Meal(
            "w11_lunsj", "Kylling quesadilla med spinat og ost", "Middag 2", 11,
            Arrays.asList(
                new Ingredient("Grove tortillas", 2, "stk"),
                new Ingredient("Grillet kyllingfilet (i strimler)", 80, "g"),
                new Ingredient("Revet lettost", 40, "g"),
                new Ingredient("Salsa", 2, "ss")
            ),
            "Legg kylling, ost og salsa på en tortilla, legg den andre oppå. Stek i tørr panne til tortillaen blir sprø og osten smelter.",
            460, 32, 38, 15,
            "Kostråd #4: Bruk mager ost for å redusere inntaket av mettet fett.",
            "🌮"
        ));

        ALL_MEALS.add(new Meal(
            "w11_middag", "Fullkornsspagetti med kyllingkjøttdeig", "Middag 3", 11,
            Arrays.asList(
                new Ingredient("Fullkornsspagetti (tørr)", 85, "g"),
                new Ingredient("Kyllingkjøttdeig", 130, "g"),
                new Ingredient("Tomatsaus med basilikum", 150, "g"),
                new Ingredient("Revet parmesan", 1, "ss")
            ),
            "Kok spagetti. Stek kyllingkjøttdeigen i en panne. Tilsett tomatsausen og la det småkoke i 5 minutter. Hell sausen over pastaen og strø over litt parmesan.",
            640, 42, 78, 14,
            "Kostråd #3: Kyllingkjøttdeig er et magrere alternativ til storfekjøttdeig i pastaretter.",
            "🍝"
        ));

        ALL_MEALS.add(new Meal(
            "w12_frokost", "Matomelett med skinke, tomat og grovbrød", "Middag 1", 12,
            Arrays.asList(
                new Ingredient("Egg", 3, "stk"),
                new Ingredient("Kokt skinke (i strimler)", 50, "g"),
                new Ingredient("Cherrytomater", 50, "g"),
                new Ingredient("Grovbrød (skive)", 1, "skive")
            ),
            "Visp egg. Stek skinke og tomater raskt i en panne, hell over eggene og stek under lokk på svak varme til omeletten stivner. Server med grovbrød.",
            440, 30, 24, 22,
            "Kostråd #1: Egg gir førsteklasses proteiner som støtter muskelreparasjon.",
            "🍳"
        ));

        ALL_MEALS.add(new Meal(
            "w12_lunsj", "Fisketaco med torsk og mangosalsa", "Middag 2", 12,
            Arrays.asList(
                new Ingredient("Torskefilet (i biter)", 120, "g"),
                new Ingredient("Tacoskjell", 2, "stk"),
                new Ingredient("Mango og rødløk", 60, "g"),
                new Ingredient("Lime (skvis)", 0.5, "stk")
            ),
            "Stek torskebiter med litt tacokrydder. Bland hakket mango, rødløk og koriander med limesaft til en frisk salsa. Fyll tacoskjellene.",
            410, 26, 48, 10,
            "Kostråd #3: Magert hvitfisk taco er en sunn og energigivende helgemiddag.",
            "🌮"
        ));

        ALL_MEALS.add(new Meal(
            "w12_middag", "Biffstrimler med ovnsbakt søtpotet", "Middag 3", 12,
            Arrays.asList(
                new Ingredient("Mager biff/ytrefilet av storfe", 120, "g"),
                new Ingredient("Søtpotet (i båter)", 180, "g"),
                new Ingredient("Brokkolini / aspargesbønner", 100, "g"),
                new Ingredient("Olivenolje", 1, "ss")
            ),
            "Bak søtpotetbåter vendt i olje, salt og pepper i ovnen på 200 °C i ca 25 minutter. Stek biffstrimlene raskt på høy varme i litt olje. Damp brokkolini de siste 3 minuttene og server.",
            580, 36, 48, 20,
            "Kostråd #3: Spis høyst 350g rødt kjøtt i uken. 120g magert storfekjøtt passer utmerket i helgen.",
            "🥩"
        ));
    }

    public static List<Meal> getAllMeals() {
        return ALL_MEALS;
    }

    public static List<Meal> getMealsForWeek(int weekNumber) {
        // Safe check to cycle the 12 weeks
        int targetWeek = ((weekNumber - 1) % 12) + 1;
        List<Meal> weekMeals = new ArrayList<>();
        for (Meal m : ALL_MEALS) {
            if (m.getWeekNumber() == targetWeek) {
                weekMeals.add(m);
            }
        }
        return weekMeals;
    }

    public static Meal getMealById(String id) {
        for (Meal m : ALL_MEALS) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        return null;
    }
}