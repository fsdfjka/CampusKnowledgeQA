export const categories = [
  { id: 1, name: '入学指南', description: '新生报到、注册、缴费等流程', count: 12, createdAt: '2026-03-01 10:20' },
  { id: 2, name: '教务教学', description: '选课、考试、成绩、学籍管理', count: 28, createdAt: '2026-03-01 10:22' },
  { id: 3, name: '奖助学金', description: '奖学金、助学金、贷款政策', count: 16, createdAt: '2026-03-01 10:25' },
  { id: 4, name: '校园生活', description: '宿舍、食堂、社团、校园卡', count: 21, createdAt: '2026-03-02 09:10' },
  { id: 5, name: '就业创业', description: '实习、招聘、就业指导', count: 9, createdAt: '2026-03-02 09:15' },
]

export const documents = [
  { id: 1, name: '2026级新生入学手册.pdf', category: '入学指南', type: 'PDF', size: '2.4 MB', chunks: 86, status: '已入库', uploader: 'admin', createdAt: '2026-03-02 09:12' },
  { id: 2, name: '本科生选课管理办法.docx', category: '教务教学', type: 'DOCX', size: '860 KB', chunks: 42, status: '已入库', uploader: 'admin', createdAt: '2026-03-03 14:02' },
  { id: 3, name: '国家奖学金评审细则.pdf', category: '奖助学金', type: 'PDF', size: '1.1 MB', chunks: 33, status: '已入库', uploader: 'teacher', createdAt: '2026-03-05 11:30' },
  { id: 4, name: '学生宿舍管理规定.txt', category: '校园生活', type: 'TXT', size: '24 KB', chunks: 12, status: '已入库', uploader: 'admin', createdAt: '2026-03-06 16:45' },
  { id: 5, name: '校园一卡通使用说明.pdf', category: '校园生活', type: 'PDF', size: '640 KB', chunks: 18, status: '解析中', uploader: 'admin', createdAt: '2026-03-08 10:05' },
  { id: 6, name: '毕业生就业推荐表填写指南.docx', category: '就业创业', type: 'DOCX', size: '320 KB', chunks: 0, status: '待解析', uploader: 'teacher', createdAt: '2026-03-09 09:20' },
]
