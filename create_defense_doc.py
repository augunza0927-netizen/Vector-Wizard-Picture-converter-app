import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

def create_document():
    doc = docx.Document()

    # Set Margins
    for section in doc.sections:
        section.top_margin = Inches(0.8)
        section.bottom_margin = Inches(0.8)
        section.left_margin = Inches(0.8)
        section.right_margin = Inches(0.8)

    # Document Header Title
    title_p = doc.add_paragraph()
    title_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run_title = title_p.add_run('VectorConvert (VectorWizard)\nOnsite Presentation & Defense Guide')
    run_title.font.name = 'Cordia New'
    run_title.font.size = Pt(24)
    run_title.bold = True
    run_title.font.color.rgb = RGBColor(0x1A, 0x36, 0x5D)

    # Subtitle
    sub_p = doc.add_paragraph()
    sub_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run_sub = sub_p.add_run('SE 233 Advanced Programming Term Project I | Chiang Mai University (CAMT)\nStudent Mode: Solo (เดี่ยว) | Anticipated Score: 14 / 14 (Full Rubric Points)')
    run_sub.font.name = 'Cordia New'
    run_sub.font.size = Pt(14)
    run_sub.font.color.rgb = RGBColor(0x4A, 0x55, 0x68)

    doc.add_paragraph()

    def add_h1(text):
        h = doc.add_paragraph()
        run = h.add_run(text)
        run.font.name = 'Cordia New'
        run.font.size = Pt(18)
        run.bold = True
        run.font.color.rgb = RGBColor(0x2B, 0x6C, 0xB0)
        h.paragraph_format.space_before = Pt(12)
        h.paragraph_format.space_after = Pt(4)
        return h

    def add_h2(text):
        h = doc.add_paragraph()
        run = h.add_run(text)
        run.font.name = 'Cordia New'
        run.font.size = Pt(15)
        run.bold = True
        run.font.color.rgb = RGBColor(0x2D, 0x37, 0x48)
        h.paragraph_format.space_before = Pt(8)
        h.paragraph_format.space_after = Pt(2)
        return h

    def add_body(text, bold_prefix=None, italic=False):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.15
        if bold_prefix:
            r_pre = p.add_run(bold_prefix)
            r_pre.font.name = 'Cordia New'
            r_pre.font.size = Pt(14)
            r_pre.bold = True
        r = p.add_run(text)
        r.font.name = 'Cordia New'
        r.font.size = Pt(14)
        r.italic = italic
        return p

    def add_bullet(text, bold_prefix=None):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(1)
        p.paragraph_format.line_spacing = 1.15
        if bold_prefix:
            r_pre = p.add_run(bold_prefix)
            r_pre.font.name = 'Cordia New'
            r_pre.font.size = Pt(14)
            r_pre.bold = True
        r = p.add_run(text)
        r.font.name = 'Cordia New'
        r.font.size = Pt(14)
        return p

    # 1. Opening Pitch
    add_h1('1. บทเปิดการนำเสนอ (Opening Pitch Script)')
    add_body('"สวัสดีครับอาจารย์ทั้งสองท่าน วันนี้ผมขอพรีเซนต์โปรเจกต์ VectorConvert (VectorWizard) ซึ่งผมพัฒนาแบบ Solo (ทำคนเดียว) โดยผมได้พัฒนาฟังก์ชันครบถ้วน 100% ตามเกณฑ์ Rubric สูงสุดของทุกหมวด โดยไม่มีการข้ามข้อแม้แต่ข้อเดียว (ไม่มีโดนหักคะแนน Skip Penalty) ผมจึงขอเสนอคะแนนประเมินตนเองที่ 14 คะแนนเต็ม (Functionality) + 1 คะแนน (UX/UI Rating) รวมเป็น 15 คะแนนเต็มครับ"', italic=True)

    # 2. Table
    add_h1('2. ตารางคะแนนที่กรอกลง Sheet (Anticipated Scores Table)')
    table = doc.add_table(rows=1, cols=3)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    hdr_cells = table.rows[0].cells
    hdr_cells[0].text = 'หัวข้อเกณฑ์ (Rubric Item)'
    hdr_cells[1].text = 'คะแนน'
    hdr_cells[2].text = 'คำอธิบายความพร้อมและหลักฐาน (Evidence)'

    for cell in hdr_cells:
        shading_elm = parse_xml(f'<w:shd {nsdecls("w")} w:fill="2B6CB0"/>')
        cell._tc.get_or_add_tcPr().append(shading_elm)
        for p in cell.paragraphs:
            for r in p.runs:
                r.font.name = 'Cordia New'
                r.font.size = Pt(13)
                r.bold = True
                r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    data = [
        ('Vectorization Format', '1', 'แปลงไฟล์ .jpg และ .png เป็น SVG ด้วย Pure JavaFX + Potrace CLI สำเร็จ 100%'),
        ('UI Navigation & View', '2', 'ซูม 3 ปุ่ม (In/Out/Fit) ซิงค์ 2 ฝั่ง + เลื่อนภาพ Back/Next พร้อมระบบ Disable สถานะปุ่ม'),
        ('Configuration: Details & BG', '2', 'ปรับ Detail ได้ 3 ระดับ (Low/Med/High) + ติ๊ก Remove Background ได้ Transparent SVG'),
        ('Configuration: Colors', '3', 'โหมด Unlimited + โหมด Custom สแกนสีอัตโนมัติ (ล็อกไม่เกิน 5, ค่าตั้งต้น 2 สี) + Instant Preview'),
        ('Zip Handling', '1', 'ลากไฟล์ .zip แตกไฟล์ในหน่วยความจำ ป้องกัน Zip-Slip และกรองเฉพาะรูปภาพเข้าคิวทันที'),
        ('Preprocessing', '2', 'รันแบบ Parallel เฉพาะเมื่อเลือก Medium + Custom (2-5 สี) พร้อม Progress Bar โดย UI ไม่ค้าง'),
        ('Export', '2', 'กด Export All เปิด Native DirectoryChooser ของ OS, เซฟไฟล์ SVG จริงลงเครื่องแบบ Parallel'),
        ('Additional feature', '1', 'เพิ่ม 2 ฟีเจอร์: (1) Theme Toggle สลับ Dark/Light mode, (2) File Size Comparison แสดงขนาดไฟล์'),
        ('Total Score', '14', 'คะแนนเต็ม 14 คะแนนหมวด Functionality (ไม่โดนหัก Skip Penalty แม้แต่ข้อเดียว)')
    ]

    for row_idx, (col0, col1, col2) in enumerate(data):
        row_cells = table.add_row().cells
        row_cells[0].text = col0
        row_cells[1].text = col1
        row_cells[2].text = col2
        bg_color = 'EDF2F7' if row_idx % 2 == 1 else 'FFFFFF'
        if row_idx == len(data) - 1:
            bg_color = 'E2E8F0'
        for c_idx, cell in enumerate(row_cells):
            shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{bg_color}"/>')
            cell._tc.get_or_add_tcPr().append(shd)
            for p in cell.paragraphs:
                for r in p.runs:
                    r.font.name = 'Cordia New'
                    r.font.size = Pt(13)
                    if row_idx == len(data) - 1 or c_idx == 1:
                        r.bold = True

    doc.add_paragraph()

    # 3. Item-by-item Defense
    add_h1('3. จุดชี้แจงรายหมวดสำหรับ Defend คะแนนกับอาจารย์ (8 หมวด)')

    add_h2('1. Vectorization Format (1 คะแนนเต็ม)')
    add_bullet(' แปลงไฟล์ภาพทั้งสกุล PNG และ JPG ให้กลายเป็น Vector SVG ผ่านเครื่องมือ Potrace ได้คมชัดสมบูรณ์', 'ความสามารถ: ')
    add_bullet(' พัฒนาด้วย Pure JavaFX 100% โดยอ่านพิกเซลผ่าน PixelReader แล้วเขียนไบนารีมาสก์ PBM P4 ไหลเข้า Potrace โดยตรง ปราศจากการเรียกใช้ java.awt.* หรือ javax.imageio.* อย่างเด็ดขาด', 'จุดเด่นเชิงเทคนิค: ')
    add_bullet(' VectorizationService.java, ColorQuantizer.java', 'ไฟล์โค้ดอ้างอิง: ')

    add_h2('2. UI Navigation & View (2 คะแนนเต็ม)')
    add_bullet(' ด้านบนมีปุ่มควบคุมเพียง 3 ปุ่มตามสเปกเป๊ะ: Zoom In, Zoom Out และ Zoom to Fit ทำงานสัมพันธ์กันทั้งภาพต้นฉบับและภาพเวกเตอร์ (Synchronized Scaling)', 'Top Panel: ')
    add_bullet(' แถบเลื่อน ScrollPane ทั้งสองฝั่งมีตัวตรวจจับเชื่อมต่อกัน (Synchronized Scrolling) เลื่อนฝั่งหนึ่ง อีกฝั่งจะเลื่อนตามทันที', 'Center Preview: ')
    add_bullet(' ปุ่ม Back และ Next ใน SettingsController มีการคำนวณตำแหน่งภาพอัตโนมัติ หากอยู่รูปแรกปุ่ม Back จะ Disable (สีเทา) และถ้าอยู่รูปสุดท้ายปุ่ม Next จะ Disable อัตโนมัติ', 'Navigation: ')

    add_h2('3. Configuration: Details & BG (2 คะแนนเต็ม)')
    add_bullet(' Detail Level มี 3 ตัวเลือกคือ Low, Medium, High ซึ่งเชื่อมโยงไปควบคุมพารามิเตอร์ Turdsize และ Alphamax ของ Potrace ส่งผลต่อความโค้งมนและรายละเอียดของเวกเตอร์จริง', 'Detail Level: ')
    add_bullet(' เมื่อเลือก Remove background ระบบจะตัดเลเยอร์สีพื้นหลังที่ตรวจจับได้ออก ส่งผลให้ SVG ที่สร้างขึ้นมีพื้นหลังโปร่งใส (Transparent SVG) และแสดงผลทันที', 'Remove Background: ')

    add_h2('4. Configuration: Colors (3 คะแนนเต็ม)')
    add_bullet(' มี 2 โหมดคือ Unlimited (ใช้สีเดิม) และ Custom (จำกัดจำนวนสี)', 'Color Modes: ')
    add_bullet(' ใช้ขั้นตอนวิธี K-Means Color Quantization ในการสแกนความถี่สีของพิกเซลจริง และตั้งค่าขีดจำกัดไม่เกิน 5 สี โดยเริ่มต้นที่ 2 สีตามข้อกำหนดอาจารย์', 'Auto-detection & Cap: ')
    add_bullet(' ทุกครั้งที่ผู้ใช้ขยับเปลี่ยนตัวเลือกสี ระบบจะสั่งสร้างภาพพรีวิวเวกเตอร์ทันทีบน Background Thread ทำให้ผู้ใช้เห็นภาพผลลัพธ์ใหม่โดยหน้าจอไม่ค้าง', 'Instant Preview: ')

    add_h2('5. Zip Handling (1 คะแนนเต็ม)')
    add_bullet(' กล่อง Drop Zone รองรับการลากไฟล์ ZIP เข้ามาโปรแกรมจะแตกไฟล์ใน Temp Directory อัตโนมัติ', 'การทำงาน: ')
    add_bullet(' มีระบบตรวจสอบพาธ ป้องกันช่องโหว่ความปลอดภัย Zip-Slip Attack และกรองเอาเฉพาะไฟล์รูปภาพ .png และ .jpg เข้าสู่คิวประมวลผล', 'ความปลอดภัย: ')
    add_bullet(' ZipExtractionService.java', 'ไฟล์โค้ดอ้างอิง: ')

    add_h2('6. Preprocessing (2 คะแนนเต็ม)')
    add_bullet(' บังคับใช้เงื่อนไขตามเกณฑ์อย่างเคร่งครัด: รันแบบ Parallel เฉพาะเมื่อเลือก Medium Detail + Custom Colors (2-5 สี) เท่านั้น หากเป็นค่าอื่นจะรันแบบ Sequential อัตโนมัติ', 'Constraint-triggered: ')
    add_bullet(' คำนวณผ่าน ThreadPool ตามจำนวนคอร์ CPU จริง บน Background Worker Thread ทำให้หน้าต่าง UI ไม่ค้าง (Non-freezing UI)', 'Multiprocessing: ')
    add_bullet(' มีหลอด ProgressBar และเปอร์เซ็นต์ความคืบหน้าแสดงผลจริงด้านล่าง (เช่น Preprocessing in parallel: 2/4 (50%)) พร้อม Callback แสดงรูปแรกทันที', 'Visual Feedback: ')
    add_bullet(' BatchProcessingManager.java (เมธอด startPreprocessing)', 'ไฟล์โค้ดอ้างอิง: ')

    add_h2('7. Export (2 คะแนนเต็ม)')
    add_bullet(' เมื่อกดปุ่ม Export All ระบบจะเรียก Native DirectoryChooser ของระบบปฏิบัติการให้ผู้ใช้เลือกโฟลเดอร์เซฟไฟล์', 'OS Dialog: ')
    add_bullet(' ประมวลผลและเซฟไฟล์ SVG จริงลงเครื่องด้วยกระบวนการ Parallel Multiprocessing พร้อมแสดงความคืบหน้าบน ProgressBar', 'Parallel Batch Export: ')
    add_bullet(' ระหว่างเซฟปุ่ม Export จะถูกล็อกไม่ให้กดซ้ำ และมี Alert แจ้งยืนยันสรุปจำนวนไฟล์ที่บันทึกสำเร็จ', 'UI Safety: ')

    add_h2('8. Additional Features (1 คะแนนเต็ม)')
    add_bullet(' สลับธีมหน้าจอระหว่าง Dark Mode และ Light Mode แบบไดนามิกผ่าน CSS สไตล์ชีต มีปุ่มรูปพระจันทร์/พระอาทิตย์บน Header (ThemeManager.java)', '1. Theme Toggle: ')
    add_bullet(' แถบ Metadata ใต้ Center Preview แสดงขนาดไฟล์จริงของ Original Image เทียบกับ Vector SVG พร้อมคำนวณอัตราส่วน Ratio % ก่อนกด Export (FileUtils.java)', '2. File Size Comparison: ')

    # 4. Differences
    add_h1('4. ข้อแตกต่างระหว่างเกณฑ์กลุ่ม 2 คน vs คนเดียว (Solo) / 3 คน')
    add_body('อาจารย์ระบุไว้ในหน้า 5 ของเอกสารโจทย์ว่า สำหรับคนทำเดี่ยว (Solo) หรือกลุ่ม 3 คน จะไม่มีคะแนนขั้นบันไดย่อย (No Partial Credit) และมีบทลงโทษ: "Plus, skipping an item from the scoring rubric will cost a 1-point penalty for each item skipped."')
    add_bullet(' หากทำไม่ครบเงื่อนไขในข้อย่อย จะไม่ได้คะแนนในข้อนั้นเลย (เช่น Colors มีเฉพาะ 0 หรือ 3 คะแนนเต็ม)', '1. กฎ All-or-Nothing: ')
    add_bullet(' หากข้ามไม่ทำข้อใดข้อหนึ่ง จะถูกหักคะแนนลบ 1 (-1 Penalty) จากคะแนนรวมทั้งหมด', '2. กฎบทลงโทษการข้ามข้อ: ')
    add_bullet(' โปรเจกต์นี้พัฒนาครบทั้ง 8 หมวดแบบ 100% ไม่มีข้อใดถูกข้าม จึงได้คะแนนเต็ม 14 คะแนนโดยไม่ถูกหักบทลงโทษใด ๆ ทั้งสิ้น', '3. สรุปความพร้อมของเรา: ')

    # 5. Live Demo Steps
    add_h1('5. สเต็ปการสาธิตสดต่อหน้าอาจารย์ (5-Minute Live Demo Steps)')
    add_bullet(' เปิดแอปขึ้นมา โชว์หน้า Drop Zone และกดปุ่ม Dark Mode / Light Mode ให้เห็นความสวยงามของ UX/UI', 'สเต็ป 1 (Launch & Theme): ')
    add_bullet(' ลากไฟล์ ZIP ที่มีรูปภาพ 3-4 รูปลงไปในกล่อง Drop Zone', 'สเต็ป 2 (ZIP & Preprocessing): ')
    add_bullet(' ชี้ให้อาจารย์ดูหลอด Progress Bar ด้านล่างที่วิ่งแสดง Preprocessing in parallel พร้อมแสดงภาพแรกขึ้นหน้าจอทันที', 'สเต็ป 3 (Parallel Feedback): ')
    add_bullet(' กดปุ่ม Zoom In, Zoom Out, Zoom to Fit และเลื่อน ScrollBar โชว์การซิงค์กัน 2 ฝั่ง จากนั้นกดปุ่ม Next / Back เลื่อนดูภาพ', 'สเต็ป 4 (View & Navigation): ')
    add_bullet(' ปรับ Detail เป็น High, ปรับจำนวนสีเป็น 3 สี และติ๊ก Remove Background โชว์ว่าภาพพรีวิวทางขวาเปลี่ยนเป็นโปร่งใสทันที', 'สเต็ป 5 (Instant Config): ')
    add_bullet(' ชี้ให้อาจารย์ดูแถบขนาดไฟล์ด้านล่าง (Original KB vs SVG KB vs Ratio %)', 'สเต็ป 6 (Size Comparison): ')
    add_bullet(' กดปุ่ม Export All เลือกโฟลเดอร์ในเครื่อง หลอดโหลดจะวิ่งบันทึกไฟล์ และเปิดโฟลเดอร์ให้อาจารย์ดูไฟล์ SVG จริง', 'สเต็ป 7 (Batch Export): ')
    add_bullet(' เปิด IntelliJ สั่งรัน AllInOneTestSuite โชว์ว่า Unit Tests ผ่าน 26/26 ข้อรวด 100%', 'สเต็ป 8 (Unit Tests): ')

    # 6. Q&A
    add_h1('6. คำถามดักคอที่อาจารย์มักจะถาม & แนวทางตอบ (Q&A FAQ)')
    add_body('ถาม: "เช็กเงื่อนไข Parallel Constraint ตรงไหนของโค้ด?"', bold_prefix='Q1: ')
    add_body('ตอบ: "อยู่ใน ConversionConfig.java เมธอด qualifiesForParallelProcessing() ครับ โดยเช็กว่า detailLevel == DetailLevel.MEDIUM && colorMode == ColorMode.CUSTOM && customColorCount >= 2 && customColorCount <= 5 และถูกดึงไปใช้ใน BatchProcessingManager.java ทั้งตอน Preprocessing และตอน Export ครับ"', bold_prefix='A1: ')

    add_body('ถาม: "มีเผลอใช้ Java AWT บ้างไหม?"', bold_prefix='Q2: ')
    add_body('ตอบ: "ไม่มีเลยครับ เป็น Pure JavaFX 100% ทั้งหมด โดยการอ่านพิกเซลใช้ JavaFX PixelReader และเขียนข้อมูลเป็นไฟล์ PBM ไบนารีด้วย BufferedOutputStream ส่งเข้า Potrace โดยตรงครับ"', bold_prefix='A2: ')

    add_body('ถาม: "ถ้ามีไฟล์รูปภาพเสียหรือเกิด Error ระหว่าง Batch Process แอปจะแครชไหม?"', bold_prefix='Q3: ')
    add_body('ตอบ: "ไม่แครชครับ มีระบบ Error Isolation ดักจับใน BatchProcessingManager ถ้ารูปใดมีปัญหาจะมาร์กสถานะเป็น FAILED และประมวลผลรูปที่เหลือต่อไปจนเสร็จ พร้อมสรุปจำนวน Error บน Status Bar ครับ"', bold_prefix='A3: ')

    # 7. Code Location Cheatsheet
    add_h1('7. แผนที่ตำแหน่งโค้ดสำหรับเปิดให้อาจารย์ดู (Exact Code Location Map)')
    add_body('เมื่ออาจารย์สั่งให้เปิดโค้ดดู สามารถใช้คีย์ลัดใน IntelliJ (กด Shift สองครั้ง หรือ Ctrl+N เพื่อพิมพ์ชื่อคลาส) แล้วชี้ไปที่บรรทัดตามตารางนี้ได้ทันทีครับ:')

    code_table = doc.add_table(rows=1, cols=4)
    code_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    c_hdr = code_table.rows[0].cells
    c_hdr[0].text = 'หมวดเกณฑ์'
    c_hdr[1].text = 'ไฟล์ (File Path)'
    c_hdr[2].text = 'ชื่อเมธอด / ฟังก์ชัน'
    c_hdr[3].text = 'บรรทัด (Lines)'

    for cell in c_hdr:
        shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="2B6CB0"/>')
        cell._tc.get_or_add_tcPr().append(shd)
        for p in cell.paragraphs:
            for r in p.runs:
                r.font.name = 'Cordia New'
                r.font.size = Pt(13)
                r.bold = True
                r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    code_data = [
        ('1. Vectorization (Pure JavaFX)', 'VectorizationService.java\nColorQuantizer.java', 'convertToSvg()\ngenerateBinaryMaskPbm()\nextractPalette() (K-Means)', '80–178\n181–218\n105–169'),
        ('2. Zoom In / Out / Fit', 'PreviewController.java', 'handleZoomIn(), handleZoomOut()\nhandleZoomFit(), applyZoom()', '97–124'),
        ('2. Synchronized Scroll', 'PreviewController.java', 'setupSynchronizedScrolling()', '61–95'),
        ('2. Back / Next Nav', 'SettingsController.java\nMainController.java', 'updateNavigationStatus()\nhandlePreviousImage(), handleNextImage()', '148–152\n213–225'),
        ('3. Detail Level (3 ระดับ)', 'SettingsController.java\nVectorizationService.java', 'detailLevelComboBox Listener\nrunPotrace() (Turdsize/Alphamax)', '53–60\n221–267'),
        ('3. Remove Background', 'SettingsController.java\nVectorizationService.java', 'removeBackgroundCheckBox Listener\nassembleCompositeSvg() (Transparent)', '91–94\n280–306'),
        ('4. Color Mode & Auto-detect', 'SettingsController.java\nColorQuantizer.java', 'setAutoDetectedColorCount() (Cap 5, Def 2)\ndetectDistinctColorCount()', '155–162\n70–102'),
        ('4. Instant Preview (No Freeze)', 'MainController.java', 'handleConfigChanged()\ngeneratePreviewForCurrent() (Task)', '261–265\n268–313'),
        ('5. ZIP Extraction & Safe', 'ZipExtractionService.java\nDropZoneController.java', 'extractImages() (Zip-Slip check)\nprocessInputFiles()', '18–75\n84–115'),
        ('6. Parallel Constraint Rule', 'ConversionConfig.java', 'qualifiesForParallelProcessing()', '53–58'),
        ('6. Parallel Preprocessing', 'BatchProcessingManager.java\nMainController.java', 'startPreprocessing() (CompletableFuture)\nhandleDroppedFiles()', '82–127\n167–211'),
        ('7. Native Export All Dialog', 'MainController.java', 'handleExportAll() (DirectoryChooser)', '93–112'),
        ('7. Parallel Batch Export', 'BatchProcessingManager.java\nMainController.java', 'startExport() (ThreadPool)\nexportToDirectory()', '130–175\n115–154'),
        ('8. Feature 1: Theme Toggle', 'ThemeManager.java\nMainController.java', 'toggleTheme(), applyTheme()\nhandleToggleTheme()', '30–48\n157–164'),
        ('8. Feature 2: Size Compare', 'FileUtils.java\nPreviewController.java', 'formatFileSize(), calculateRatio()\ndisplayItem() (Size & Ratio Labels)', '34–48\n154–160'),
        ('Test Suite (26 Tests PASS)', 'AllInOneTestSuite.java', '@Suite @SelectClasses (รวม 8 คลาส 26 ข้อ)', '1–22')
    ]

    for row_idx, (c0, c1, c2, c3) in enumerate(code_data):
        row = code_table.add_row().cells
        row[0].text = c0
        row[1].text = c1
        row[2].text = c2
        row[3].text = c3
        bg = 'EDF2F7' if row_idx % 2 == 1 else 'FFFFFF'
        for cell in row:
            shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{bg}"/>')
            cell._tc.get_or_add_tcPr().append(shd)
            for p in cell.paragraphs:
                for r in p.runs:
                    r.font.name = 'Cordia New'
                    r.font.size = Pt(12)

    out_file = os.path.abspath('Defense_Guide_VectorConvert.docx')
    doc.save(out_file)
    print('SUCCESS: Created', out_file)

if __name__ == '__main__':
    create_document()
