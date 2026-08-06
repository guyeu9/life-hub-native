package com.lifehub.util

import com.lifehub.LifeHubApplication
import com.lifehub.data.SettingsRepository
import com.lifehub.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * 演示数据生成器，对齐 HTML 版 seedDemo()
 */
object DemoDataUtil {

    suspend fun seed(app: LifeHubApplication) = withContext(Dispatchers.IO) {
        val c = app.container

        // 1. 设置
        c.settings.setBudget(6000.0)
        c.settings.setNetRebate(false)
        c.settings.setFields(SettingsRepository.FieldTable())
        c.settings.setFitnessProfile(
            SettingsRepository.FitnessProfile(height = 172.0, startWeight = 0.0, targetWeight = 63.0, tdee = 1580.0)
        )

        // 2. 账目（跨 3 个月）
        val now = System.currentTimeMillis()
        val ledger = listOf(
            LedgerEntity(type = "expense", category = "餐饮", amount = 34.0, note = "楼下面馆的辣肉面", date = dayOffsetMillis(0)),
            LedgerEntity(type = "expense", category = "居家", amount = 128.5, note = "洗衣液和纸巾囤货", date = dayOffsetMillis(-1)),
            LedgerEntity(type = "expense", category = "交通", amount = 19.0, note = "地铁通勤", date = dayOffsetMillis(-2)),
            LedgerEntity(type = "expense", category = "购物", amount = 268.0, note = "一双跑鞋", date = dayOffsetMillis(-4)),
            LedgerEntity(type = "expense", category = "娱乐", amount = 399.0, note = "livehouse 门票", date = dayOffsetMillis(-6)),
            LedgerEntity(type = "expense", category = "学习", amount = 88.0, note = "一本书 + 一杯咖啡", date = dayOffsetMillis(-8)),
            LedgerEntity(type = "income", category = "工资", amount = 13500.0, note = "本月薪水", date = dayOffsetMillis(-10)),
            LedgerEntity(type = "expense", category = "居家", amount = 2200.0, note = "房租", date = dayOffsetMillis(-12)),
            LedgerEntity(type = "expense", category = "医疗", amount = 156.0, note = "感冒去了趟社区医院", date = dayOffsetMillis(-16)),
            LedgerEntity(type = "expense", category = "居家", amount = 2200.0, note = "上月房租", date = dayOffsetMillis(-34)),
            LedgerEntity(type = "expense", category = "餐饮", amount = 820.0, note = "上月吃饭合计", date = dayOffsetMillis(-38)),
            LedgerEntity(type = "income", category = "工资", amount = 13500.0, note = "上月薪水", date = dayOffsetMillis(-40)),
            LedgerEntity(type = "expense", category = "居家", amount = 2200.0, note = "房租", date = dayOffsetMillis(-62)),
            LedgerEntity(type = "income", category = "工资", amount = 13500.0, note = "薪水", date = dayOffsetMillis(-66)),
            // 返利
            LedgerEntity(type = "rebate", category = "堂食返利", amount = 3.4, note = "面馆扫码立减", rebateOf = 34.0, date = dayOffsetMillis(0)),
            LedgerEntity(type = "rebate", category = "外卖返利", amount = 12.0, note = "外卖红包 + 会员返", rebateOf = 68.0, date = dayOffsetMillis(-1)),
            LedgerEntity(type = "rebate", category = "团购优惠", amount = 26.8, note = "两人火锅套餐比原价省的", rebateOf = 268.0, date = dayOffsetMillis(-3)),
            LedgerEntity(type = "rebate", category = "外卖返利", amount = 18.5, note = "公司订餐平台返", rebateOf = 126.0, date = dayOffsetMillis(-7)),
            LedgerEntity(type = "rebate", category = "信用卡返现", amount = 60.0, note = "餐饮类目 5% 返现", rebateOf = 1200.0, date = dayOffsetMillis(-11)),
            LedgerEntity(type = "rebate", category = "堂食返利", amount = 9.9, note = "咖啡第二杯半价折算", rebateOf = 45.0, date = dayOffsetMillis(-15)),
            LedgerEntity(type = "rebate", category = "外卖返利", amount = 88.0, note = "上月外卖返利合计", rebateOf = 960.0, date = dayOffsetMillis(-36)),
            LedgerEntity(type = "rebate", category = "外卖返利", amount = 71.0, note = "外卖返利合计", rebateOf = 880.0, date = dayOffsetMillis(-64))
        )
        c.ledger.insertAll(ledger)

        // 3. 习惯
        val h1 = HabitEntity(name = "早睡（23:30 前）", type = "check", target = 1, unit = "", color = "#5D7561", createdAt = dayOffsetMillis(-30))
        val h2 = HabitEntity(name = "喝水", type = "count", target = 8, unit = "杯", color = "#4A6478", createdAt = dayOffsetMillis(-30))
        val h3 = HabitEntity(name = "阅读", type = "value", target = 30, unit = "分钟", color = "#A2543C", createdAt = dayOffsetMillis(-30))
        val habitIds = c.habit.insertAllHabits(listOf(h1, h2, h3))
        val logs = mutableListOf<HabitLogEntity>()
        for (i in 1..29) {
            val day = dayOffsetKey(-i)
            if (prand(i, 11.3) > 0.28) logs.add(HabitLogEntity(habitId = habitIds[0], dateKey = day, done = true, value = 1.0))
            logs.add(HabitLogEntity(habitId = habitIds[1], dateKey = day, done = false, value = (4 + prand(i, 7.7) * 5).roundToInt().toDouble()))
            val v = (prand(i, 3.1) * 46).roundToInt()
            if (v > 6) logs.add(HabitLogEntity(habitId = habitIds[2], dateKey = day, done = v >= 30, value = v.toDouble()))
        }
        // 今天喝水只记 3 杯，留一个未完成
        logs.add(HabitLogEntity(habitId = habitIds[1], dateKey = todayKey(), done = false, value = 3.0))
        c.habit.insertAllLogs(logs)

        // 4. 身体记录（14 天，今天不预填）
        var w = 68.9
        val fitnessList = mutableListOf<FitnessEntity>()
        for (j in 13 downTo 1) {
            w = w - 0.055 + (prand(j, 5.5) - 0.5) * 0.34
            val weight = round1(w)
            val fat = round1(22.4 - (13 - j) * 0.045 + (prand(j, 9.1) - 0.5) * 0.5)
            val intake = (1500 + prand(j, 2.2) * 520).roundToInt().toDouble()
            val burn = (180 + prand(j, 6.4) * 420).roundToInt().toDouble()
            fitnessList.add(FitnessEntity(dateKey = dayOffsetKey(-j), weight = weight, fat = fat, intake = intake, burn = burn, date = dayOffsetMillis(-j)))
        }
        c.fitness.insertAll(fitnessList)

        // 周计划
        val weekPlan = listOf(
            FitnessPlanEntity(0, "力量 · 上肢", "卧推 / 划船 / 推举，各 4 组"),
            FitnessPlanEntity(1, "有氧 · 慢跑", "5 公里，配速 6'30\" 以内"),
            FitnessPlanEntity(2, "力量 · 下肢", "深蹲 / 硬拉 / 弓步，各 4 组"),
            FitnessPlanEntity(3, "休息 · 拉伸", "20 分钟全身拉伸 + 早睡"),
            FitnessPlanEntity(4, "力量 · 核心", "平板支撑 / 卷腹 / 死虫，各 3 组"),
            FitnessPlanEntity(5, "户外 · 长距离", "骑车或快走 60 分钟"),
            FitnessPlanEntity(6, "自由 · 放空", "想动就动，不想动就好好吃饭")
        )
        c.fitnessPlan.upsertAll(weekPlan)

        // 5. 日程（含 1 条逾期）
        val schedules = listOf(
            ScheduleEntity(title = "交房租水电费", due = dayOffsetMillis(-2), tag = "生活", priority = "P0"),
            ScheduleEntity(title = "给爸妈打个电话", due = dayOffsetMillis(0), time = "20:00", tag = "家人", priority = "P1"),
            ScheduleEntity(title = "把冬天的衣服收进箱子", due = dayOffsetMillis(0), tag = "家务", priority = "P2"),
            ScheduleEntity(title = "牙科复查", due = dayOffsetMillis(3), time = "10:30", tag = "健康", priority = "P1"),
            ScheduleEntity(title = "取快递", due = dayOffsetMillis(-1), tag = "生活", priority = "P2", done = true)
        )
        c.schedule.insertAll(schedules)

        // 6. 待买
        val wishes = listOf(
            WishItemEntity(name = "一盏落地灯", estPrice = 459.0, priority = "P1", note = "客厅角落太暗了", createdAt = dayOffsetMillis(-5)),
            WishItemEntity(name = "磨豆机", estPrice = 329.0, priority = "P2", note = "手冲用，先观望", createdAt = dayOffsetMillis(-9)),
            WishItemEntity(name = "跑鞋", estPrice = 268.0, priority = "P0", note = "旧的底磨平了", bought = true, createdAt = dayOffsetMillis(-4))
        )
        c.wish.insertAll(wishes)

        // 7. 书影音
        val media = listOf(
            MediaItemEntity(type = "book", title = "长安的荔枝", status = "done", rating = 4f, review = "一口气读完，小人物的困兽之斗写得很扎实。", color = "#A2543C", finishDate = dayOffsetKey(-11), updatedAt = now),
            MediaItemEntity(type = "movie", title = "银翼杀手 2049", status = "done", rating = 5f, review = "画面和配乐都在说同一件事：孤独。", color = "#4A6478", finishDate = dayOffsetKey(-25), updatedAt = now),
            MediaItemEntity(type = "book", title = "置身事内", status = "doing", color = "#5D7561", updatedAt = now),
            MediaItemEntity(type = "music", title = "范玫瑰 · 房东的猫", status = "done", rating = 4f, review = "通勤路上单曲循环了一周。", color = "#A8842F", finishDate = dayOffsetKey(-40), updatedAt = now),
            MediaItemEntity(type = "movie", title = "完美的日子", status = "want", color = "#8D6B52", updatedAt = now)
        )
        c.media.insertAll(media)
    }

    private fun dayOffsetMillis(offset: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, offset)
        return cal.timeInMillis
    }

    private fun dayOffsetKey(offset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, offset)
        return "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    private fun round1(v: Double): Double = (v * 10).roundToInt() / 10.0

    // 轻量确定性伪随机，保证演示数据每次一致
    private fun prand(i: Int, seed: Double): Double {
        val x = kotlin.math.sin((i + 1) * seed) * 43758.5453
        return x - kotlin.math.floor(x)
    }
}
