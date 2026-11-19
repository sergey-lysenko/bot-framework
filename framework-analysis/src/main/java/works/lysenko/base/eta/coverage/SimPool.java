package works.lysenko.base.eta.coverage;

/**
 * Immutable precomputed simulation pool containing primitive-indexed candidates and static base weights.
 *
 * @param candidateIds      primitive scenario IDs of candidates in this pool
 * @param baseWeights       precalculated static base weights for candidates
 * @param descScenarioIds   array mapping candidate index to descendant scenario IDs
 * @param descLeafIds       array mapping candidate index to descendant leaf IDs
 * @param hasRepeatableLeaf flag per candidate indicating presence of non-Mono descendant leaves
 * @param childPools        nested simulation pools for node candidates, null for leaf candidates
 */
record SimPool(
        int[] candidateIds,
        double[] baseWeights,
        int[][] descScenarioIds,
        int[][] descLeafIds,
        boolean[] hasRepeatableLeaf,
        SimPool[] childPools
) {}
