package works.lysenko.util.func.data;

import com.github.javafaker.Faker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Collector.selectOneOf;
import static works.lysenko.util.spec.Symbols._QUOTE_;

/**
 * Set of wrapper routines for {@link com.github.javafaker} library
 */
@SuppressWarnings({"MethodCallInLoopCondition", "StandardVariableNames", "MethodWithMultipleReturnPoints"})
public record Fakers() {

    /**
     * @return random character from one of nineteen
     * {@link com.github.javafaker.Faker} fictional character generators
     */
    public static String character() {

        final List<String> s = new ArrayList<>(1);
        s.add(new Faker().backToTheFuture().character());
        s.add(new Faker().buffy().characters());
        s.add(new Faker().dune().character());
        s.add(new Faker().elderScrolls().creature());
        s.add(new Faker().friends().character());
        s.add(new Faker().gameOfThrones().character());
        s.add(new Faker().harryPotter().character());
        s.add(new Faker().hitchhikersGuideToTheGalaxy().character());
        s.add(new Faker().hobbit().character());
        s.add(new Faker().howIMetYourMother().character());
        s.add(new Faker().lebowski().character());
        s.add(new Faker().lordOfTheRings().character());
        s.add(new Faker().overwatch().hero());
        s.add(new Faker().princessBride().character());
        s.add(new Faker().rickAndMorty().character());
        s.add(new Faker().starTrek().character());
        s.add(new Faker().twinPeaks().character());
        s.add(new Faker().witcher().character());
        s.add(new Faker().zelda().character());
        String candidate;
        do {
            candidate = selectOneOf(s);
        } while (candidate.contains(s(_QUOTE_)));
        return candidate;
    }

    /**
     * {@link Fakers#quote()} with array of character sequences to be excluded.
     *
     * @param exclude sequences of characters not to be included in generated text
     * @return random quote
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static String quote(final CharSequence... exclude) {

        String candidate;
        do
            candidate = quote();
        while (Arrays.stream(exclude).anyMatch(candidate::contains));
        return candidate;
    }

    /**
     * {@link Fakers#quote()} with array of character sequences
     * to be excluded and with limited length. Content is just trimmed to requested size.
     *
     * @param maxLength of the generated quote string. Any value less than 1 to ignore limit
     * @param exclude   sequences of characters not to be included in generated text
     * @return generated quote
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String quote(final int maxLength, final CharSequence... exclude) {

        String candidate;
        if (1 < exclude.length)
            do
                candidate = quote();
            while (Arrays.stream(exclude).anyMatch(candidate::contains));
        else
            candidate = quote();
        if (0 > maxLength)
            return candidate;
        else
            return candidate.substring(0, Math.min(candidate.length(), maxLength));
    }


    /**
     * @return random quote from one of twenty-one
     * {@link com.github.javafaker.Faker} quote generators
     */
    public static String quote() {

        final List<String> s = new ArrayList<>(1);
        s.add(new Faker().backToTheFuture().quote());
        s.add(new Faker().buffy().quotes());
        s.add(new Faker().dune().quote());
        s.add(new Faker().elderScrolls().quote());
        s.add(new Faker().friends().quote());
        s.add(new Faker().gameOfThrones().quote());
        s.add(new Faker().harryPotter().quote());
        s.add(new Faker().hitchhikersGuideToTheGalaxy().quote());
        s.add(new Faker().hobbit().quote());
        s.add(new Faker().howIMetYourMother().quote());
        s.add(new Faker().leagueOfLegends().quote());
        s.add(new Faker().lebowski().quote());
        s.add(new Faker().overwatch().quote());
        s.add(new Faker().princessBride().quote());
        s.add(new Faker().rickAndMorty().quote());
        s.add(new Faker().robin().quote());
        s.add(new Faker().shakespeare().asYouLikeItQuote());
        s.add(new Faker().shakespeare().hamletQuote());
        s.add(new Faker().shakespeare().kingRichardIIIQuote());
        s.add(new Faker().shakespeare().romeoAndJulietQuote());
        s.add(new Faker().yoda().quote());
        String candidate;
        do {
            candidate = selectOneOf(s);
        } while (candidate.contains("'"));
        return candidate;
    }
}
