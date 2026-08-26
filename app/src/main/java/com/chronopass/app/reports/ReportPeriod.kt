package com.chronopass.app.reports

// Atalhos de período do filtro de relatorios. Mes atual/passado vem primeiro:
// folha de pagamento é mensal, é o que se exporta na maioria das vezes.
enum class ReportPeriod(val label: String) {
    THIS_MONTH("Este mês"),
    LAST_MONTH("Mês passado"),
    LAST_7("7 dias"),
    LAST_30("30 dias"),
    CUSTOM("Escolher datas");

    // ponytail: CUSTOM cai no dia de hoje até o usuário escolher no calendário.
    fun range(now: Long = System.currentTimeMillis()): Pair<Long, Long> = when (this) {
        THIS_MONTH -> TimeUtil.startOfMonth(now) to TimeUtil.endOfDay(now)
        LAST_MONTH -> TimeUtil.addMonths(now, -1)
            .let { TimeUtil.startOfMonth(it) to TimeUtil.endOfMonth(it) }
        LAST_7 -> TimeUtil.startOfDay(TimeUtil.addDays(now, -6)) to TimeUtil.endOfDay(now)
        LAST_30 -> TimeUtil.startOfDay(TimeUtil.addDays(now, -29)) to TimeUtil.endOfDay(now)
        CUSTOM -> TimeUtil.startOfDay(now) to TimeUtil.endOfDay(now)
    }
}
