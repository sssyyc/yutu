<template>
  <div class="route-detail-page">
    <section class="page-card product-shell">
      <div class="gallery-panel">
        <div class="gallery-stage">
          <button
            v-if="previewImages.length > 1"
            type="button"
            class="gallery-nav gallery-nav-prev"
            aria-label="Previous image"
            @click.stop="showPreviousImage"
          >
            <span aria-hidden="true">&#8249;</span>
          </button>

          <el-image
            v-if="activeGalleryImage"
            :src="activeGalleryImage"
            fit="cover"
            class="gallery-main-image"
            :preview-src-list="previewImages"
            preview-teleported
          />
          <div v-else class="gallery-main-empty">暂无路线图片</div>

          <button
            v-if="previewImages.length > 1"
            type="button"
            class="gallery-nav gallery-nav-next"
            aria-label="Next image"
            @click.stop="showNextImage"
          >
            <span aria-hidden="true">&#8250;</span>
          </button>

          <div class="gallery-badges">
            <span class="gallery-kicker">ROUTE GALLERY</span>
            <span class="gallery-count">{{ previewImages.length }} 张预览图</span>
          </div>
        </div>

        <div v-if="previewImages.length > 1" class="gallery-strip">
          <button
            v-for="(img, index) in previewImages"
            :key="`${img}-${index}`"
            type="button"
            class="thumb-item"
            :class="{ active: img === activeGalleryImage }"
            @click="activeGalleryImage = img"
          >
            <img :src="img" alt="路线缩略图" class="thumb-image" />
          </button>
        </div>
      </div>

      <div class="purchase-panel">
        <div class="info-header">
          <div class="panel-topline light">
            <span class="panel-meta-text">路线详情</span>
            <span class="panel-divider">·</span>
            <span class="panel-meta-text">{{ routeCategoryName }}</span>
            <span class="panel-divider">·</span>
            <span class="panel-meta-text">已有{{ reviewCount }}条评价</span>
          </div>

          <h1 class="route-title">{{ routeInfo.routeName || "路线详情" }}</h1>
          <p class="route-summary">{{ routeInfo.summary || "暂无路线简介" }}</p>
        </div>

        <div v-if="routeTags.length" class="tag-row">
          <el-tag
            v-for="item in routeTags"
            :key="item.id"
            effect="light"
            round
            type="success"
          >
            {{ item.tagName }}
          </el-tag>
        </div>

        <div class="rating-strip">
          <span class="rating-star">★</span>
          <span class="rating-value">{{ displayScore }}</span>
          <span class="rating-text">综合评分</span>
          <span class="rating-divider">·</span>
          <span class="rating-text">好评率{{ positiveReviewRateText }}</span>
        </div>

        <div class="booking-card">
          <div class="booking-card-main">
            <div class="price-block">
              <div class="board-label">参考价格</div>
              <div class="board-price-row">
                <span class="board-currency">￥</span>
                <span class="board-price">{{ displayPrice }}</span>
                <span class="board-unit">起/人</span>
              </div>
            </div>

            <div class="info-list">
              <div class="info-row selectable">
                <span class="info-row-label">出发日期</span>
                <span class="info-row-value">{{ selectedDate?.departDate || nextDeparture?.departDate || "待更新" }}</span>
              </div>
              <div class="info-row">
                <span class="info-row-label">可选批次</span>
                <span class="info-row-value">{{ dates.length }}</span>
              </div>
              <div class="info-row">
                <span class="info-row-label">当前余位</span>
                <span class="info-row-value">{{ selectedDate ? selectedDate.remainCount : "--" }}</span>
              </div>
            </div>
          </div>

          <div class="purchase-actions">
            <el-button
              class="book-btn"
              type="primary"
              :disabled="!canOpenBooking"
              @click="openSelectedOrder"
            >
              {{ bookingButtonText }}
            </el-button>
            <el-button class="secondary-btn" @click="scrollToReviews">查看评价</el-button>
          </div>

          <div class="purchase-tip-bar">
            <span class="purchase-tip-icon">i</span>
            <p class="purchase-tip">
              {{ bookingTip }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <section ref="detailSectionRef" class="page-card detail-card">
      <div class="section-head">
        <div>
          <p class="section-kicker">DETAILS</p>
          <h3 class="section-title">路线详情</h3>
        </div>
      </div>

      <div class="detail-copy">
        {{ detailText || "暂无详细介绍" }}
      </div>

      <div v-if="scheduleList.length" class="schedule-list">
        <article
          v-for="item in scheduleList"
          :key="item.id"
          class="schedule-card"
        >
          <div class="schedule-day">DAY {{ item.dayNo || "-" }}</div>
          <div class="schedule-body">
            <h4>{{ item.title || `第${item.dayNo || "-"}天行程` }}</h4>
            <p>{{ item.content || "暂无详细行程说明" }}</p>
          </div>
        </article>
      </div>
    </section>

    <section ref="reviewsSectionRef" class="page-card review-card">
      <div class="section-head review-head">
        <div>
          <p class="section-kicker">REVIEWS</p>
          <h3 class="section-title">用户评价</h3>
          <h3 class="section-title">闂傚倸鍊搁崐鐑芥倿閿曗偓椤啴宕归鍛姺闂佺鍕垫當缂佲偓婢跺备鍋撻獮鍨姎妞わ富鍨跺浼村Ψ閿斿墽顔曢梺鐟邦嚟閸嬬喖銆傞弻銉﹀€垫慨姗嗗幖閸濇椽鏌＄仦鍓ф创鐎殿喗鎸虫俊鎼佸Χ閸ャ劍娅﹀┑</h3>
        </div>

        <div class="review-summary-panel">
          <div class="review-score">{{ displayScore }}</div>
          <div class="review-summary-copy">
            <el-rate
              :model-value="displayScoreNumber"
              disabled
              allow-half
              text-color="#f59e0b"
              disabled-void-color="#d6dbe4"
            />
            <span>{{ reviewCount }} 条真实评价</span>
            <span>{{ reviewCount }} 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曞熬閵娾晛鐒垫い鎺戝€荤壕濂稿级閸稑濡界紒鈧埀顒勬⒑閹稿孩纾搁柛銊ょ矙瀵偄顓兼径濠勵槹闂侀潧顭堥崕鐗堢珶閺囥垺鈷戦梻鍫氭櫅閻︽粓鏌涘Ο鍝勬倯缂佸倹甯￠崺锟犲川椤旀儳骞楅梻浣瑰濞插秹宕戦幘缁樺€垫繛鎴炲笚濞呭洨绱</span>
          </div>
        </div>
      </div>

      <div v-if="reviewList.length" class="review-list">
        <article v-for="item in reviewList" :key="item.id" class="review-item">
          <el-avatar
            :src="item.avatar || undefined"
            :size="48"
            class="review-avatar"
          >
            {{ reviewInitial(item.displayName) }}
          </el-avatar>

          <div class="review-body">
            <div class="review-meta">
              <div>
                <div class="review-user">{{ item.displayName || "匿名用户" }}</div>
                <div class="review-date">{{ formatDateTime(item.createTime) }}</div>
              </div>

              <el-rate
                :model-value="Number(item.score) || 0"
                disabled
                text-color="#f59e0b"
                disabled-void-color="#d6dbe4"
              />
            </div>

            <p class="review-content">
              {{ item.content || "该用户没有填写文字评价" }}
            </p>
          </div>
        </article>
      </div>

      <el-empty
        v-else
        description="暂时还没有用户评价"
      />
    </section>

    <el-dialog v-model="orderDialog" title="创建订单" width="860px" class="order-dialog">
      <div class="order-dialog-body">
        <div class="dialog-section">
          <div class="dialog-section-head">
            <h4>选择预约日期</h4>
            <span class="dialog-section-tip">先选日期，再选择出行人</span>
          </div>

          <div v-if="dates.length" class="dialog-date-grid">
            <button
              v-for="item in dates"
              :key="item.id"
              type="button"
              class="dialog-date-option"
              :class="{
                active: Number(selectedDateId) === Number(item.id),
                disabled: Number(item.remainCount) <= 0
              }"
              :disabled="Number(item.remainCount) <= 0"
              @click="selectDate(item)"
            >
              <span class="dialog-date-day">{{ item.departDate }}</span>
              <span class="dialog-date-meta">{{ departureHint(item.departDate) }}</span>
              <span class="dialog-date-price">￥ {{ formatPrice(item.salePrice) }}</span>
              <span class="dialog-date-stock">
                {{ Number(item.remainCount) > 0 ? `剩余 ${item.remainCount} 位` : "已售罄" }}
              </span>
            </button>
          </div>

          <el-empty v-else description="当前暂无可预约日期" />
        </div>

        <div class="order-info-row">
          <span class="info-label">路线名称</span>
          <span class="info-value">{{ routeInfo.routeName || "-" }}</span>
        </div>

        <div class="order-info-row">
          <span class="info-label">出发日期</span>
          <span class="info-value">{{ selectedDate?.departDate || "-" }}</span>
        </div>

        <div class="order-info-row">
          <span class="info-label">出发价格</span>
          <span class="info-value">￥ {{ selectedDate ? formatPrice(selectedDate.salePrice) : "--" }}</span>
        </div>

        <div class="traveler-select-wrap">
          <div class="traveler-toolbar">
            <span>请选择本次出行人（已选 {{ selectedTravelers.length }} 人）</span>
            <el-button text type="primary" @click="toTravelerPage">去管理出行人</el-button>
          </div>

          <el-table
            ref="travelerTableRef"
            :data="travelers"
            border
            class="traveler-select-table"
            max-height="320"
            @selection-change="handleTravelerSelectionChange"
          >
            <el-table-column type="selection" width="56" />
            <el-table-column prop="travelerName" label="姓名" min-width="130" />
            <el-table-column prop="idCard" label="身份证号" min-width="260" show-overflow-tooltip />
            <el-table-column prop="phone" label="手机号" min-width="150" />
          </el-table>

          <el-empty v-if="!loadingTravelers && travelers.length === 0" description="暂无出行人，请先添加" />
        </div>
      </div>

      <template #footer>
        <el-button @click="orderDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingOrder" @click="createOrder">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api } from "../../api";
import { useAuthStore } from "../../stores/auth";
import { filterUpcomingDepartures, getDaysUntilDeparture } from "../../utils/departureDate";
import { parseRouteDetailContent } from "../../utils/routeDetailMeta";

const ID_CARD_18_REGEX = /^\d{17}[\dXx]$/;

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const detail = ref({});
const dates = ref([]);
const travelers = ref([]);
const selectedTravelers = ref([]);
const loadingTravelers = ref(false);
const creatingOrder = ref(false);
const orderDialog = ref(false);
const selectedDateId = ref(null);
const activeGalleryImage = ref("");
const travelerTableRef = ref(null);
const detailSectionRef = ref(null);
const reviewsSectionRef = ref(null);

const routeInfo = computed(() => detail.value?.route || {});
const routeCategoryName = computed(() => detail.value?.category?.categoryName || "未分类");
const routeTags = computed(() => (Array.isArray(detail.value?.tags) ? detail.value.tags : []));
const scheduleList = computed(() => (Array.isArray(detail.value?.schedules) ? detail.value.schedules : []));
const reviewList = computed(() => (Array.isArray(detail.value?.reviews) ? detail.value.reviews : []));
const parsedDetail = computed(() => parseRouteDetailContent(routeInfo.value.detailContent));
const detailText = computed(() => parsedDetail.value.plainText);
const scenicImages = computed(() => parsedDetail.value.relatedImages || []);

const previewImages = computed(() => {
  const images = [];
  const append = (url) => {
    const value = String(url || "").trim();
    if (!value || images.includes(value)) {
      return;
    }
    images.push(value);
  };
  append(routeInfo.value.coverImage);
  scenicImages.value.forEach(append);
  return images;
});

const activeImageIndex = computed(() => {
  return previewImages.value.findIndex((item) => item === activeGalleryImage.value);
});

const nextDeparture = computed(() => {
  return dates.value.find((item) => Number(item.remainCount) > 0) || dates.value[0] || null;
});

const selectedDate = computed(() => {
  return dates.value.find((item) => Number(item.id) === Number(selectedDateId.value)) || null;
});

const canCreateOrder = computed(() => {
  return auth.hasPerm("order:create");
});

const canOpenBooking = computed(() => {
  return canCreateOrder.value && dates.value.length > 0;
});

const bookingButtonText = computed(() => {
  if (!canCreateOrder.value) {
    return "仅游客可预订";
  }
  if (!dates.value.length) {
    return "暂无可约日期";
  }
  return "立即预订";
});

const displayPrice = computed(() => {
  return formatPrice(selectedDate.value?.salePrice ?? nextDeparture.value?.salePrice ?? routeInfo.value.price);
});

const reviewCount = computed(() => reviewList.value.length);

const averageReviewScore = computed(() => {
  if (reviewList.value.length) {
    const total = reviewList.value.reduce((sum, item) => sum + (Number(item.score) || 0), 0);
    return Number((total / reviewList.value.length).toFixed(1));
  }
  const fallback = Number(routeInfo.value.score);
  return Number.isFinite(fallback) && fallback > 0 ? Number(fallback.toFixed(1)) : 0;
});

const displayScoreNumber = computed(() => {
  return averageReviewScore.value > 0 ? averageReviewScore.value : 0;
});

const displayScore = computed(() => {
  return displayScoreNumber.value > 0 ? displayScoreNumber.value.toFixed(1) : "--";
});

const positiveReviewRateText = computed(() => {
  if (!reviewList.value.length) {
    return "暂无";
  }
  const positiveCount = reviewList.value.filter((item) => Number(item.score) >= 4).length;
  return `${Math.round((positiveCount / reviewList.value.length) * 100)}%`;
});

const bookingTip = computed(() => {
  if (!canCreateOrder.value) {
    return "当前账号暂无下单权限，请切换可预订账号下单。";
  }
  if (!selectedDate.value) {
    return "请选择一个可预约的出发日期后再提交订单。";
  }
  return `已选 ${selectedDate.value.departDate}，下单后将进入合同签署流程。`;
});

function formatPrice(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return "--";
  }
  return Number.isInteger(num) ? String(num) : num.toFixed(2);
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 19);
}

/*
function departureHint(value) {
  const days = getDaysUntilDeparture(value);
  if (days === null) {
    return "闂傚倷娴囬褎顨ラ幖浣瑰€舵慨姗嗗墻閻斿棙鎱ㄥ璇蹭壕濡ょ姷鍋涢敃顏堢嵁閸ヮ剙绾ч悹鎭掑妺缁辨粍绻濈喊妯活潑闁搞劋鍗冲畷銉р偓锝庡亜閸?;
  }
  if (days <= 0) {
    return "婵犵數濮烽弫鎼佸磻濞戙埄鏁嬫い鎾跺枑閸欏繘鏌熺紒銏犳灍闁哄懏绻堥弻鏇㈠醇濠垫劖笑婵℃鎳忕换婵嬪閿濆懐鍘梺鍛婃⒐濞茬喖骞嗙仦杞挎梹鎷呴崗鍝ョ泿闂備礁鎼粔鏌ュ礉鎼淬劍鍎楀┑鐘插暔娴?;
  }
  if (days === 1) {
    return "闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢妶鍌氫壕婵鍘ф晶顖涖亜閵婏絽鍔︽鐐寸墬閹峰懘鎳栧┑鎾剁暠妞ゎ厼娼￠幊婊堟濞戞﹩娼撶紓鍌欒閸嬫捇鏌涢弴銊ョ仭闁绘挻娲樼换婵嬫濞戞瑯妫炲銈呯箚閺呮繄妲?;
  }
  return `${days} 婵犵數濮烽弫鍛婃叏娴兼潙鍨傜憸鐗堝笚閸婂爼鏌涢鐘插姎闁汇倗鍋撶换娑㈠箣濞嗗繒浠肩紓浣哄У閻楁绌辨繝鍥ч柛娑卞枛濞咃絿绱撴担鎻掍壕闂佸憡娲﹂崹閬嶆偂閺囩喓绡€闂傚牊绋掗ˉ婊勩亜韫囧﹥娅囩紒杈ㄥ浮閹崇娀顢楁径濠冾唴;
}

function reviewInitial(name) {
  const text = String(name || "濠?).trim();
  return text ? text.slice(0, 1).toUpperCase() : "濠?;
}

*/

function departureHint(value) {
  const days = getDaysUntilDeparture(value);
  if (days === null) {
    return "日期待定";
  }
  if (days <= 0) {
    return "已过期";
  }
  if (days === 1) {
    return "明天出发";
  }
  return `${days} 天后出发`;
}

function reviewInitial(name) {
  const text = String(name || "游客").trim();
  return text ? text.slice(0, 1).toUpperCase() : "游";
}

function syncSelectedDate() {
  const currentExists = dates.value.some((item) => Number(item.id) === Number(selectedDateId.value));
  if (currentExists) {
    return;
  }
  const defaultDate = dates.value.find((item) => Number(item.remainCount) > 0) || dates.value[0] || null;
  selectedDateId.value = defaultDate?.id ?? null;
}

async function load() {
  const id = route.params.id;
  const [detailResp, dateResp] = await Promise.all([
    api.get(`/routes/${id}`),
    api.get(`/routes/${id}/dates`)
  ]);
  detail.value = detailResp || {};
  dates.value = filterUpcomingDepartures(dateResp);
  syncSelectedDate();
}

async function loadTravelers() {
  loadingTravelers.value = true;
  try {
    travelers.value = await api.get("/travelers");
  } finally {
    loadingTravelers.value = false;
  }
}

function selectDate(item) {
  if (!item || Number(item.remainCount) <= 0) {
    return;
  }
  selectedDateId.value = item.id;
}

function scrollToReviews() {
  reviewsSectionRef.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

/*
async function openSelectedOrder() {
  if (auth.user?.roleType !== 1) {
    ElMessage.info("闂傚倷娴囧畷鐢稿窗閹邦喖鍨濋幖娣灪濞呯姵淇婇妶鍛櫣缂佺姳鍗抽弻娑樷槈濮楀牊鏁惧┑鐐叉噽婵炩偓闁哄矉绲借灒闁惧繘鈧稓椹抽梺璇插閼归箖鈥﹂悜钘夎摕闁靛ň鏅涢崡铏繆閵堝倸浜炬繛瀛樼矒缁犳牠骞冨畡閭︾叆闁告侗鍙庨弳顓㈡煟鎼达絿鎳楅柛娑卞灣閻掑潡姊洪崷顓炲妺妞ゃ劌鎳庨蹇撯攽閸ャ儰绨婚梺鍝勫暙濞层倛顣挎繝鐢靛仜閻楀懘宕￠崘宸綎濡わ箒锟ユ禍褰掓煙閻戞ɑ灏伴柣婵嗙－缁辨挻鎷呴挊澶屽帿閻庡厜鍋撻柟闂寸杩濋梺绋跨灱閸嬫盯鎮″☉銏＄厱闁硅埇鍔嶅▍鍛存煛鐎ｎ亜鈧灝顫忓ú顏勭闁圭粯甯婃竟鏇犵磽娴ｇ懓绲绘繛纭风節楠炲啴濮€閵堝懎绐涘銈嗙墬缁酣鏁嶅鍫熲拺閻犲洠鈧磭鈧鏌涢幇鍏哥凹妞ゆ梹鍔楃槐鎾诲磼濞嗘劗銈版俊鐐存綑閹芥粓骞戦姀銈呯妞ゆ棁濮ゅ▍鏍⒑閸︻厼顣兼繝銏☆焽缁牏鈧綆鍋佹禍婊堟煛閸モ晛浠﹂柛锝呯秺閺岀喖顢欓崗鐓庢畻闂佸搫鐭夌紞渚€骞冮埡鍛煑濠㈣泛顑嗛崐鐑芥⒒娴ｈ鍋犻柛銊︽そ閹繝鍨惧畷鍥ㄦ缂佺虎鍘奸幊鎰版偪閳ь剙鈹戦悙鏉戠仸妞ゎ厼娲、娆愬緞閹邦厸鎷洪梻鍌氱墛閻╊垶鎮板鍛＜閺夊牄鍔嶇粈瀣偓瑙勬礀閹碱偉鐏冮梺鍛婁緱閸犳牠鍩€椤掆偓閻忔岸銆冮妷鈺傚€烽柤纰卞厸閾忓酣姊虹拠鑼鐎规洦鍓熼垾锔炬崉閵婏箑纾梺鍛婄箓鐎氶攱瀵奸崘顔藉€?);
    return;
  }
  if (!dates.value.length) {
    ElMessage.warning("闂傚倷娴囧畷鐢稿窗閹邦喖鍨濋幖娣灪濞呯姵淇婇妶鍛櫣缂佺姳鍗抽弻娑樷槈濮楀牊鏁惧┑鐐叉噽婵炩偓闁哄矉绲借灒婵炲棙鍎冲▓顓炩攽椤旀娼愰柣鎿勭節瀵鈽夊顐ｅ媰闂佸憡鎸嗛埀顒佹叏閸ヮ剚鈷戦悹鎭掑妼閺嬪倿鏌涙惔锝嗘毈鐎殿喛顕ч埥澶娢熼柨瀣偓璇测攽閳藉棗鐏ユ繛鍜冪稻缁傛帞绮欏▎鐐瘜闂侀潧鐗嗙换鎺戠暆濞戙垺鐓涢柛娑卞枤缁犵偤鏌熼妤€浜炬俊鐐€曠换鎰版偋婵犲洤鐤炬繝濠傜墛閻撶娀鏌熼鐔风瑨闁告梻鏁哥槐鎺撴綇閵娿儱鎽靛┑?);
    return;
  }

  selectedTravelers.value = [];
  await loadTravelers();
  orderDialog.value = true;
  await nextTick();
  travelerTableRef.value?.clearSelection();
}

function handleTravelerSelectionChange(rows) {
  selectedTravelers.value = rows || [];
}

function toTravelerPage() {
  orderDialog.value = false;
  router.push("/user/travelers");
}

async function createOrder() {
  if (!selectedDate.value?.id) {
    ElMessage.warning("闂傚倸鍊峰ù鍥х暦閸偅鍙忛柡澶嬪殮濞差亜惟闁宠桨鑳堕ˇ褍鈹戦濮愪粶闁稿鎹囬弻娑㈠煘閹傚濠碉紕鍋戦崐鏍ь啅婵犳艾纾婚柟鐐暘娴滄粓鏌ㄩ弮鍥棄妞ゃ儱顑夐弻宥堫檨闁告挻宀稿畷顐ｆ償閵娿儳顦梺鐟扮摠缁诲嫰寮抽敂鐣岀闁糕剝蓱鐏忣厼霉閼测晛鈻堥柡灞剧洴瀵挳濡搁妷銈囨晼闁诲骸鍘滈崑鎾寸箾閹存瑥鐏柍閿嬪灴閺屾稑鈹戦崱妤婁患闂佸搫妫欏Λ鍐蓟濞戞瑦鍎熼柕蹇曞Х缁佺兘姊?);
    return;
  }
  if (!selectedTravelers.value.length) {
    ElMessage.warning("闂傚倸鍊峰ù鍥х暦閸偅鍙忛柡澶嬪殮濞差亜围濠㈣泛锕ょ花銉╂⒑閼测斁鎷￠柛蹇旓耿瀹曟垿骞樼紒妯绘珳闂佺鏈惌顔界珶閺囥垺鈷掗柛灞捐壘閳ь剟顥撳▎銏ゆ晸閻樿尙锛涢梺鍛婃处閸ㄤ線宕堕鈧粻顕€鏌﹀Ο渚Ц闁诡垳鍋ゅ娲箰鎼淬垻鍙嗛悷婊勫閸嬨倝骞?1 婵犵數濮烽弫鎼佸磻閻樿绠垫い蹇撴缁躲倝鏌ｉ敐鍛伇闁活厽鎹囬弻鐔虹磼閵忕姵鐏堥梺鍛婁亢椤濡甸崟顖氱閻犺櫣娲呴敐澶嬬厱閻庯綆鍋撻懓鍧楁煙椤旀枻鑰块柟顔界懇楠炴捇骞掗幘鏂ュ亾椤栫偞鈷?);
    return;
  }
  const hasInvalidIdCard = selectedTravelers.value.some((item) => !ID_CARD_18_REGEX.test(String(item.idCard ?? "").trim()));
  if (hasInvalidIdCard) {
    ElMessage.warning("闂傚倸鍊搁崐椋庣矆娴ｉ潻鑰块梺顒€绉查埀顒€鍊圭粋鎺斺偓锝庝簽閿涙盯姊洪悷鏉库挃缂侇噮鍨堕崺娑㈠箣閻愵亙绨婚梺瑙勫閺呮盯鎮為悜妯圭箚闁告瑥顦伴崐鎰版煛鐏炲墽娲村┑陇鍩栭幆鏃堝灳瀹曞浂鍟嬮梻鍌欐祰椤曟牠宕板Δ浣虹彾闁糕剝顭囬々鐑芥煥閺囩偛鈧摜绮堥崒鐐寸厾婵炴潙顑嗗▍鍡欑磼閵娧冧槐婵﹨娅ｉ幏鐘诲箵閹烘繂濡风紓鍌欑椤戝棛鏁敓鐘叉瀬閻庯綆浜栭弨浠嬫煟濮楀棗浜滃ù婊勫劤椤啴濡堕崨顖滎唶闂佺粯顨呯换妯虹暦閿濆鍗抽柣鏃傜節缁ㄥ姊虹涵鍛劷闁告柨顦靛畷鎴﹀箻缂佹ɑ鍎銈嗗姧缁查箖鎮鹃崼鏇熲拻濞达綀妫勯崥鐟扳攽椤旇姤缍戦悡銈夋煥濠靛棙宸濋柣顓炴闇夐柨婵嗘媼濞肩喎霉濠婂啰绉洪柡宀€鍠撶划娆撳箰鎼淬垹闂梻浣哥－缁垰顫忔繝姘劦妞ゆ巻鍋撶紒鐘茬Ч瀹曟洟宕￠悘璇茬秺瀹曟帡鎮欓懠顒婄吹濠电姷鏁告慨鏉懨洪敃鍌涘亗?18 婵犵數濮烽弫鎼佸磻閻樿绠垫い蹇撴缁躲倝鏌ｉ敐鍛伇闁活厽鎹囬弻锝夊箣閻愬浼勭紓鍌氱У閻楁洟鈥﹂崸妤佸殝闂傚牊绋戦～宀€绱撴担鎻掍壕闂佺硶鍓濋…鍥╃不妤ｅ啯鐓曢柍鈺佸枤閻掍粙鏌熼崘鍙夊櫧缂佽鲸甯￠崺鈧い鎺嶇缁剁偤鏌熼柇锕€骞栧ù鐙€鍨跺娲箹閻愭彃濡ч梺鍛婂姀閺佲晠鍩￠崨顔规嫼闂佸憡绋戦…鈧柟杈鹃檮閸庡孩銇勯弽銊︾殤闁哄棴闄勯幈銊ノ熼幐搴ｃ€愮紓浣哄珡閸ャ劎鍘卞銈庡幗閸ㄧ敻寮搁悢鍏肩厽闁挎棁濮らˉ鍫ユ煛瀹€瀣М闁诡喗鐟ч埀顒傛暩绾泛危閸喓绡€闂傚牊绋戦埀顒€缍婂濠氬Ω閿旇姤鐝℃繝鐢靛О閸ㄧ厧鈻斿☉銏″剶闁稿繗鍋愰埢鏃傗偓骞垮劚椤︿即鎮￠妷锔剧闁瑰浼濋鍫晜妞ゅ繐鐗婇悡鏇炩攽閻樻彃顏╁ù鐘崇矋閵囧嫰顢旈崟顐ｆ婵犵鈧磭鍩ｇ€规洏鍔戦、姗€鎮㈢亸浣镐壕閹兼番鍔嶉埛鎴︽⒑椤愶絿銆掗柍瑙勫浮閺屾盯寮埀顒勫垂閻㈠壊鏁?);
    return;
  }

  creatingOrder.value = true;
  try {
    const payload = await api.post("/orders", {
      routeId: Number(route.params.id),
      departDateId: selectedDate.value.id,
      travelerCount: selectedTravelers.value.length,
      travelers: selectedTravelers.value.map((item) => ({
        travelerName: item.travelerName,
        idCard: item.idCard,
        phone: item.phone
      }))
    });
    ElMessage.success("婵犵數濮烽弫鎼佸磻閻愬搫鍨傞柛顐ｆ礀缁犱即鏌熼梻瀵歌窗闁轰礁瀚伴弻娑樷槈閸楃偛绠婚梺鍝勬４缁犳捇寮婚敐澶婄睄闁稿本鑹炬禒妯肩磽娴ｅ搫顎撶紓宥勭窔瀵鎮㈤崗鑲╁姺闂佹寧娲嶉崑鎾愁熆瑜滈崰妤呭Φ?);
    orderDialog.value = false;
    if (payload?.contractId) {
      router.push(`/contract/detail/${payload.contractId}`);
      return;
    }
    router.push(`/order/detail/${payload?.orderId}`);
  } finally {
    creatingOrder.value = false;
  }
}

*/

async function openSelectedOrder() {
  if (!canCreateOrder.value) {
    ElMessage.info("当前账号暂无下单权限");
    return;
  }
  if (!dates.value.length) {
    ElMessage.warning("当前暂无可预约日期");
    return;
  }

  selectedTravelers.value = [];
  await loadTravelers();
  orderDialog.value = true;
  await nextTick();
  travelerTableRef.value?.clearSelection();
}

function handleTravelerSelectionChange(rows) {
  selectedTravelers.value = rows || [];
}

function toTravelerPage() {
  orderDialog.value = false;
  router.push("/user/travelers");
}

async function createOrder() {
  if (!selectedDate.value?.id) {
    ElMessage.warning("请选择出发日期");
    return;
  }
  if (!selectedTravelers.value.length) {
    ElMessage.warning("请先选择出行人");
    return;
  }
  const hasInvalidIdCard = selectedTravelers.value.some((item) => !ID_CARD_18_REGEX.test(String(item.idCard ?? "").trim()));
  if (hasInvalidIdCard) {
    ElMessage.warning("存在身份证号格式不正确的出行人，请检查");
    return;
  }

  creatingOrder.value = true;
  try {
    const payload = await api.post("/orders", {
      routeId: Number(route.params.id),
      departDateId: selectedDate.value.id,
      travelerCount: selectedTravelers.value.length,
      travelers: selectedTravelers.value.map((item) => ({
        travelerName: item.travelerName,
        idCard: item.idCard,
        phone: item.phone
      }))
    });
    ElMessage.success("订单创建成功，即将进入合同签署流程");
    orderDialog.value = false;
    if (payload?.contractId) {
      router.push(`/contract/detail/${payload.contractId}`);
      return;
    }
    router.push(`/order/detail/${payload?.orderId}`);
  } finally {
    creatingOrder.value = false;
  }
}

function showPreviousImage() {
  if (previewImages.value.length <= 1) {
    return;
  }
  const currentIndex = activeImageIndex.value >= 0 ? activeImageIndex.value : 0;
  const previousIndex =
    currentIndex === 0 ? previewImages.value.length - 1 : currentIndex - 1;
  activeGalleryImage.value = previewImages.value[previousIndex];
}

function showNextImage() {
  if (previewImages.value.length <= 1) {
    return;
  }
  const currentIndex = activeImageIndex.value >= 0 ? activeImageIndex.value : 0;
  const nextIndex =
    currentIndex === previewImages.value.length - 1 ? 0 : currentIndex + 1;
  activeGalleryImage.value = previewImages.value[nextIndex];
}

watch(
  previewImages,
  (images) => {
    if (images.includes(activeGalleryImage.value)) {
      return;
    }
    activeGalleryImage.value = images[0] || "";
  },
  { immediate: true }
);

watch(
  () => route.params.id,
  async () => {
    await load();
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }
);

onMounted(load);
</script>

<style scoped>
.route-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.92fr);
  gap: 22px;
  border: 1px solid #e4ebf4;
}

.gallery-panel,
.purchase-panel {
  display: flex;
  flex-direction: column;
}

.gallery-stage {
  position: relative;
  overflow: hidden;
  border-radius: 22px;
  background: linear-gradient(135deg, #0f172a 0%, #111827 100%);
  aspect-ratio: 16 / 10;
}

.gallery-main-image {
  width: 100%;
  height: 100%;
  display: block;
}

.gallery-main-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.82);
  font-size: 16px;
}

.gallery-nav {
  position: absolute;
  top: 50%;
  z-index: 2;
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.42);
  color: #ffffff;
  cursor: pointer;
  transform: translateY(-50%);
  transition: background 0.2s ease, transform 0.2s ease;
  backdrop-filter: blur(8px);
}

.gallery-nav:hover {
  background: rgba(15, 23, 42, 0.64);
  transform: translateY(-50%) scale(1.04);
}

.gallery-nav span {
  font-size: 28px;
  line-height: 1;
}

.gallery-nav-prev {
  left: 18px;
}

.gallery-nav-next {
  right: 18px;
}

.gallery-badges {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.gallery-kicker,
.gallery-count {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  color: #f8fafc;
  font-size: 12px;
  letter-spacing: 0.12em;
  background: rgba(15, 23, 42, 0.56);
  backdrop-filter: blur(8px);
}

.gallery-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(88px, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.thumb-item {
  padding: 0;
  border: 1px solid #d9e3ef;
  border-radius: 16px;
  overflow: hidden;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  aspect-ratio: 16 / 10;
}

.thumb-item:hover {
  border-color: #5ba2f7;
  transform: translateY(-1px);
}

.thumb-item.active {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.14);
}

.thumb-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.purchase-panel {
  gap: 18px;
}

.info-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-topline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.panel-topline.light {
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.panel-meta-text {
  color: #64748b;
  font-size: 13px;
}

.panel-divider {
  color: #94a3b8;
}

.route-title {
  margin: 0;
  color: #0f172a;
  font-size: clamp(32px, 2.2vw, 44px);
  line-height: 1.15;
}

.route-summary {
  margin: 0;
  color: #334155;
  font-size: 17px;
  line-height: 1.8;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rating-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.rating-star {
  color: #f59e0b;
  font-size: 14px;
}

.rating-value {
  color: #0f172a;
  font-weight: 700;
}

.rating-text {
  color: #64748b;
}

.rating-divider {
  color: #cbd5f5;
}

.booking-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.booking-card-main {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.board-label {
  color: #64748b;
  font-size: 14px;
}

.price-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #fff6ed 0%, #eef7ff 100%);
  border: 1px solid #f2d8c0;
}

.board-price-row {
  display: flex;
  align-items: flex-end;
  gap: 6px;
}

.board-currency {
  color: #d97706;
  font-size: 22px;
  font-weight: 700;
}

.board-price {
  color: #d97706;
  font-size: clamp(36px, 3vw, 54px);
  line-height: 1;
  font-weight: 800;
}

.board-unit {
  color: #94a3b8;
  font-size: 13px;
}

.info-list {
  display: grid;
  gap: 10px;
  padding: 10px 0;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #eef2f7;
  color: #334155;
  font-size: 14px;
}

.info-row:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.info-row-label {
  color: #94a3b8;
}

.info-row-value {
  color: #0f172a;
  font-weight: 600;
}

.info-row.selectable .info-row-value {
  color: #2563eb;
}

.purchase-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.book-btn,
.secondary-btn {
  min-width: 168px;
  height: 48px;
  border-radius: 14px;
}

.secondary-btn {
  color: #2563eb;
  border-color: #bfdbfe;
}

.purchase-tip-bar {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 13px;
}

.purchase-tip-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.purchase-tip {
  margin: 0;
  line-height: 1.6;
}

.detail-card,
.review-card {
  border: 1px solid #e4ebf4;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-kicker {
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 12px;
  letter-spacing: 0.16em;
  font-weight: 700;
}

.section-title {
  margin: 0;
  color: #0f172a;
  font-size: clamp(24px, 1.8vw, 32px);
  line-height: 1.2;
}

.detail-copy {
  padding: 20px 22px;
  border-radius: 22px;
  background: linear-gradient(180deg, #fbfdff 0%, #f7fafc 100%);
  border: 1px solid #e2e8f0;
  color: #334155;
  font-size: 16px;
  line-height: 1.9;
  white-space: pre-wrap;
}

.schedule-list {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.schedule-card {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 16px;
  padding: 18px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.schedule-day {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 92px;
  border-radius: 18px;
  background: linear-gradient(135deg, #0f766e 0%, #1d4ed8 100%);
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
}

.schedule-body h4 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.schedule-body p {
  margin: 10px 0 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.review-head {
  align-items: center;
}

.review-head .section-title + .section-title {
  display: none;
}

.review-summary-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 22px;
  background: linear-gradient(135deg, #fff7ed 0%, #fffaf2 100%);
  border: 1px solid #fed7aa;
}

.review-score {
  min-width: 88px;
  color: #d97706;
  font-size: 38px;
  line-height: 1;
  font-weight: 800;
}

.review-summary-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.review-summary-copy span + span {
  display: none;
}

.review-list {
  display: grid;
  gap: 14px;
}

.review-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px;
  padding: 18px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.review-avatar {
  background: linear-gradient(135deg, #e0f2fe 0%, #dbeafe 100%);
  color: #0f172a;
  font-weight: 700;
}

.review-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.review-user {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.review-date {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 13px;
}

.review-content {
  margin: 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.order-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 2px;
}

.dialog-section {
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.dialog-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.dialog-section-head h4 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

.dialog-section-tip {
  color: #64748b;
  font-size: 12px;
}

.dialog-date-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.dialog-date-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid #d7e2ef;
  border-radius: 16px;
  background: #ffffff;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.dialog-date-option:hover:not(.disabled) {
  border-color: #60a5fa;
  transform: translateY(-1px);
}

.dialog-date-option.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.dialog-date-option.disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.dialog-date-day {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.dialog-date-meta,
.dialog-date-stock {
  color: #64748b;
  font-size: 12px;
}

.dialog-date-price {
  color: #d97706;
  font-size: 16px;
  font-weight: 700;
}

.order-info-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.info-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
}

.info-value {
  font-size: 16px;
  color: #0f172a;
  font-weight: 600;
}

.traveler-select-wrap {
  width: 100%;
}

.traveler-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: #334155;
  font-size: 15px;
  font-weight: 500;
}

.traveler-select-table :deep(.el-table__header th) {
  height: 46px;
}

.traveler-select-table :deep(.el-table__row td) {
  height: 58px;
}

@media (max-width: 1280px) {
  .product-shell {
    grid-template-columns: 1fr;
  }

  .gallery-main-image,
  .gallery-stage,
  .gallery-main-empty {
    aspect-ratio: 16 / 10;
  }
}

@media (max-width: 960px) {
  .review-head,
  .section-head,
  .review-meta,
  .dialog-section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .booking-card-main,
  .dialog-date-grid {
    grid-template-columns: 1fr;
  }

  .schedule-card {
    grid-template-columns: 1fr;
  }

  .schedule-day {
    min-height: 72px;
  }

  .review-item {
    grid-template-columns: 1fr;
  }

  .gallery-nav {
    width: 38px;
    height: 38px;
  }

  .gallery-nav span {
    font-size: 24px;
  }
}

@media (max-width: 760px) {
  :deep(.order-dialog .el-dialog) {
    width: min(95vw, 860px) !important;
  }

  .route-title {
    font-size: clamp(28px, 8vw, 36px);
  }

  .gallery-main-image,
  .gallery-stage,
  .gallery-main-empty {
    aspect-ratio: 16 / 10;
  }

  .traveler-toolbar,
  .purchase-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .book-btn,
  .secondary-btn {
    width: 100%;
  }

  .gallery-nav {
    width: 34px;
    height: 34px;
  }

  .gallery-nav-prev {
    left: 12px;
  }

  .gallery-nav-next {
    right: 12px;
  }
}
</style>
