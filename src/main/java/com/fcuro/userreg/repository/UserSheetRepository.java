package com.fcuro.userreg.repository;

import com.fcuro.userreg.domain.User;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Googleスプレッドシートの指定シート(タブ)をユーザデータストアとして扱うリポジトリ。
 *
 * どのスプレッドシート・どのシート(タブ)を対象とするかはログイン画面・登録画面でユーザが
 * 入力するため、固定値としては保持せず、呼び出しのたびにspreadsheetId・sheetNameを引数で受け取る。
 *
 * シート構成(ヘッダ行を1行目に設置し、2行目以降がデータ):
 * A列: allowId(内部識別子・ヘッダー名は可変) / B列: email / C列: password
 * D列: targetId(任意) / E列: retiredFlag / F列: adminFlag / G列: updatedAt
 */
@Repository
public class UserSheetRepository {

    private static final Logger log = LoggerFactory.getLogger(UserSheetRepository.class);

    private static final int COL_ALLOW_ID = 0;
    private static final int COL_EMAIL = 1;
    private static final int COL_PASSWORD = 2;
    private static final int COL_TARGET_ID = 3;
    private static final int COL_RETIRED_FLAG = 4;
    private static final int COL_ADMIN_FLAG = 5;
    private static final int COL_UPDATED_AT = 6;

    private final Sheets sheetsService;

    public UserSheetRepository(Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    public Optional<User> findByEmail(String spreadsheetId, String sheetName, String email) {
        return readAllRows(spreadsheetId, sheetName).stream()
                .filter(row -> !row.isEmpty() && email.equalsIgnoreCase(cell(row, COL_EMAIL)))
                .findFirst()
                .map(this::toUser);
    }

    public boolean existsByEmail(String spreadsheetId, String sheetName, String email) {
        return findByEmail(spreadsheetId, sheetName, email).isPresent();
    }

    public List<User> findAll(String spreadsheetId, String sheetName) {
        return readAllRows(spreadsheetId, sheetName).stream()
                .filter(row -> !cell(row, COL_EMAIL).isEmpty())
                .map(this::toUser)
                .toList();
    }

    /**
     * ユーザを1行追加する。allowIdは既存データ件数から1始まりの連番を採番し、
     * 書き込み先の行も既存データ件数から自前で計算する(ヘッダ1行 + 既存データ件数 + 1行目)。
     *
     * Sheets APIのappend()は指定範囲内で値が入っているとみなされた行の直後に書き込む仕様のため、
     * チェックボックスの書式等で見た目上は空でも値ありと判定される列があると、大きく離れた行に
     * 書き込まれることがある。それを避けるため、実データ(email列)の件数から書き込み先行を
     * 直接指定するupdate()を使う。
     */
    public void save(String spreadsheetId, String sheetName, User user) {
        List<List<Object>> existingRows = readAllRows(spreadsheetId, sheetName);
        int dataRowCount = countDataRows(existingRows);
        String allowId = String.valueOf(dataRowCount + 1);
        int targetRow = dataRowCount + 2;

        ValueRange body = new ValueRange().setValues(List.of(List.of(
                allowId,
                user.getEmail(),
                user.getPassword(),
                user.getTargetId() != null ? user.getTargetId() : "",
                String.valueOf(user.isRetired()),
                String.valueOf(user.isAdmin()),
                user.getUpdatedAt()
        )));
        try {
            String range = quote(sheetName) + "!A" + targetRow + ":G" + targetRow;
            UpdateValuesResponse response = sheetsService.spreadsheets().values()
                    .update(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute();
            log.debug("スプレッドシート書き込み結果: spreadsheetId=[{}] range=[{}] updatedRange=[{}] updatedRows=[{}]",
                    spreadsheetId, range, response.getUpdatedRange(), response.getUpdatedRows());
        } catch (IOException e) {
            throw new SheetAccessException("スプレッドシートへのユーザ登録に失敗しました", e);
        }
    }

    /**
     * allowIdでユーザを特定し、retiredFlag・updatedAt列を更新する(adminFlagは現在の値を維持する)。
     * 対象のallowIdが見つからなかった場合はfalseを返す。
     */
    public boolean updateRetiredFlag(String spreadsheetId, String sheetName, String allowId, boolean retired,
                                      String updatedAt) {
        List<List<Object>> rows = readAllRows(spreadsheetId, sheetName);
        int targetRow = findRowIndex(rows, allowId);
        if (targetRow == -1) {
            return false;
        }
        String currentAdminFlag = cell(rows.get(targetRow - 2), COL_ADMIN_FLAG);
        writeFlags(spreadsheetId, sheetName, targetRow, String.valueOf(retired), currentAdminFlag, updatedAt,
                "退職フラグ");
        return true;
    }

    /**
     * allowIdでユーザを特定し、adminFlag・updatedAt列を更新する(retiredFlagは現在の値を維持する)。
     * 対象のallowIdが見つからなかった場合はfalseを返す。
     */
    public boolean updateAdminFlag(String spreadsheetId, String sheetName, String allowId, boolean admin,
                                    String updatedAt) {
        List<List<Object>> rows = readAllRows(spreadsheetId, sheetName);
        int targetRow = findRowIndex(rows, allowId);
        if (targetRow == -1) {
            return false;
        }
        String currentRetiredFlag = cell(rows.get(targetRow - 2), COL_RETIRED_FLAG);
        writeFlags(spreadsheetId, sheetName, targetRow, currentRetiredFlag, String.valueOf(admin), updatedAt,
                "管理者フラグ");
        return true;
    }

    /** 指定行のretiredFlag(E列)・adminFlag(F列)・updatedAt(G列)をまとめて書き込む。 */
    private void writeFlags(String spreadsheetId, String sheetName, int targetRow, String retiredFlag,
                             String adminFlag, String updatedAt, String fieldLabel) {
        ValueRange body = new ValueRange().setValues(List.of(List.of(retiredFlag, adminFlag, updatedAt)));
        try {
            String range = quote(sheetName) + "!E" + targetRow + ":G" + targetRow;
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute();
        } catch (IOException e) {
            throw new SheetAccessException(fieldLabel + "の更新に失敗しました", e);
        }
    }

    /** allowIdに一致する行のシート上の行番号(1始まり、ヘッダ行込み)を返す。見つからない場合は-1。 */
    private int findRowIndex(List<List<Object>> rows, String allowId) {
        for (int i = 0; i < rows.size(); i++) {
            if (allowId.equals(cell(rows.get(i), COL_ALLOW_ID))) {
                return i + 2;
            }
        }
        return -1;
    }

    /** email列が空でない行数を実データ件数として数える(書式のみのセルを実データと誤認しないため)。 */
    private int countDataRows(List<List<Object>> rows) {
        int count = 0;
        for (List<Object> row : rows) {
            if (!cell(row, COL_EMAIL).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private List<List<Object>> readAllRows(String spreadsheetId, String sheetName) {
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, readRange(sheetName))
                    .execute();
            List<List<Object>> values = response.getValues();
            List<List<Object>> rows = values != null ? values : List.of();
            log.debug("スプレッドシート読み込み結果: spreadsheetId=[{}] requestRange=[{}] 行数=[{}]",
                    spreadsheetId, readRange(sheetName), rows.size());
            return rows;
        } catch (IOException e) {
            throw new SheetAccessException("スプレッドシートの読み込みに失敗しました", e);
        }
    }

    private String readRange(String sheetName) {
        return quote(sheetName) + "!A2:G";
    }

    /** シート名をA1記法の範囲指定で使えるよう、単一引用符で囲む(名前中の単一引用符は2重化してエスケープ)。 */
    private String quote(String sheetName) {
        return "'" + sheetName.replace("'", "''") + "'";
    }

    private User toUser(List<Object> row) {
        return new User(
                cell(row, COL_ALLOW_ID),
                cell(row, COL_EMAIL),
                cell(row, COL_PASSWORD),
                cell(row, COL_TARGET_ID),
                Boolean.parseBoolean(cell(row, COL_RETIRED_FLAG)),
                Boolean.parseBoolean(cell(row, COL_ADMIN_FLAG)),
                cell(row, COL_UPDATED_AT)
        );
    }

    private String cell(List<Object> row, int index) {
        return index < row.size() && row.get(index) != null ? String.valueOf(row.get(index)) : "";
    }
}
