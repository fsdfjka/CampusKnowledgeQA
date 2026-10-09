export const studentProfile = {
  username: 'student',
  nickname: '张三',
  studentNo: '20260101',
  college: '计算机与信息科学学院',
  major: '软件工程',
  email: 'zhangsan@example.com',
  phone: '138****8888',
}

export const categories = [
  { id: 1, name: '入学指南', description: '新生报到、注册、缴费等流程', count: 12 },
  { id: 2, name: '教务教学', description: '选课、考试、成绩、学籍管理', count: 28 },
  { id: 3, name: '奖助学金', description: '奖学金、助学金、贷款政策', count: 16 },
  { id: 4, name: '校园生活', description: '宿舍、食堂、社团、校园卡', count: 21 },
  { id: 5, name: '就业创业', description: '实习、招聘、就业指导', count: 9 },
]

export const documents = [
  { id: 1, name: '2026级新生入学手册.pdf', category: '入学指南', type: 'PDF', size: '2.4 MB', uploadTime: '2026-03-02', summary: '涵盖报到流程、材料清单、缴费方式、户口迁移等新生入学全部事项。' },
  { id: 2, name: '本科生选课管理办法.docx', category: '教务教学', type: 'DOCX', size: '860 KB', uploadTime: '2026-03-03', summary: '选课时间安排、学分要求、退改选规则与特殊情况处理办法。' },
  { id: 3, name: '国家奖学金评审细则.pdf', category: '奖助学金', type: 'PDF', size: '1.1 MB', uploadTime: '2026-03-05', summary: '国家奖学金申请条件、评审流程、材料要求与公示安排。' },
  { id: 4, name: '学生宿舍管理规定.txt', category: '校园生活', type: 'TXT', size: '24 KB', uploadTime: '2026-03-06', summary: '宿舍作息、用电安全、卫生检查、来访登记等管理规定。' },
  { id: 5, name: '校园一卡通使用说明.pdf', category: '校园生活', type: 'PDF', size: '640 KB', uploadTime: '2026-03-08', summary: '校园卡充值、消费、挂失、补办流程与常见问题。' },
  { id: 6, name: '毕业生就业推荐表填写指南.docx', category: '就业创业', type: 'DOCX', size: '320 KB', uploadTime: '2026-03-09', summary: '就业推荐表填写规范、盖章流程与注意事项。' },
]

export const hotQuestions = [
  { id: 1, question: '新生报到需要带哪些材料？', count: 320, category: '入学指南' },
  { id: 2, question: '转专业需要满足什么条件？', count: 268, category: '教务教学' },
  { id: 3, question: '国家奖学金的申请条件是什么？', count: 245, category: '奖助学金' },
  { id: 4, question: '宿舍晚上几点熄灯？', count: 198, category: '校园生活' },
  { id: 5, question: '校园卡丢了怎么补办？', count: 176, category: '校园生活' },
  { id: 6, question: '每学期选课什么时候开始？', count: 142, category: '教务教学' },
]

export const qaHistory = [
  {
    id: 1,
    question: '新生报到需要带哪些材料？',
    answer: '需携带录取通知书、身份证原件及复印件、高考准考证、一寸免冠照片若干张，以及党团组织关系证明。',
    sources: ['2026级新生入学手册.pdf'],
    feedback: '点赞',
    favorite: true,
    createdAt: '2026-03-10 09:12:33',
  },
  {
    id: 2,
    question: '国家奖学金的申请条件是什么？',
    answer: '需为全日制在校本科生，学习成绩排名与综合考评排名均位于前 10%，且无违纪记录。',
    sources: ['国家奖学金评审细则.pdf'],
    feedback: '点赞',
    favorite: false,
    createdAt: '2026-03-10 10:02:11',
  },
  {
    id: 3,
    question: '宿舍晚上几点熄灯？',
    answer: '宿舍楼周日至周四 23:00 熄灯，周五、周六 23:30 熄灯。',
    sources: ['学生宿舍管理规定.txt'],
    feedback: '-',
    favorite: false,
    createdAt: '2026-03-10 11:40:02',
  },
  {
    id: 4,
    question: '转专业需要满足什么条件？',
    answer: '大一学年无挂科记录，且转入专业有接收名额，需通过转入学院组织的考核。',
    sources: ['本科生选课管理办法.docx'],
    feedback: '点踩',
    favorite: false,
    createdAt: '2026-03-11 08:55:20',
  },
]

export const sensitiveWords = ['违禁词', '暴恐', '代考', '黄牛']
